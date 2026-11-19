package com.example.blooddonor.integration;

import com.example.blooddonor.dto.MatchStatusRequest;
import com.example.blooddonor.entity.*;
import com.example.blooddonor.repository.*;
import com.example.blooddonor.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

/**
 * Step 3.1: 32-Thread Concurrency Test for Donor Acceptance on MySQL Testcontainer.
 *
 * Verifies that under extreme concurrency (32 simultaneous threads):
 * 1. Exactly the required number of units (unitsRequired = 2) can be accepted (HTTP 200).
 * 2. Excess simultaneous accept attempts receive HTTP 409 Conflict ("Enough donors have already accepted").
 * 3. Database state remains 100% consistent: exactly 2 matches are ACCEPTED, 30 remain MATCHED.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Accept32ThreadTest extends AbstractMySqlIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalProfileRepository hospitalProfileRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private EmergencyRequestRepository emergencyRequestRepository;

    @Autowired
    private RequestDonorMatchRepository requestDonorMatchRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void cleanDatabase() {
        notificationRepository.deleteAll();
        requestDonorMatchRepository.deleteAll();
        emergencyRequestRepository.deleteAll();
        donorProfileRepository.deleteAll();
        hospitalProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("32-Thread Concurrency: 32 concurrent donor accepts for a 2-unit request must yield exactly 2 OKs (200) and 30 Conflicts (409)")
    void concurrentDonorAccepts_mustNeverExceedUnitsRequiredAndExcessMustReturn409() throws Exception {
        // 1. Arrange: Create Hospital and EmergencyRequest
        User hospitalUser = new User();
        hospitalUser.setName("Metropolitan General Hospital");
        hospitalUser.setEmail("hospital_accept32@lifelink.com");
        hospitalUser.setPassword("$2a$10$dummyhashedpasswordfortestingpurposes");
        hospitalUser.setRole(Role.HOSPITAL);
        hospitalUser = userRepository.save(hospitalUser);

        HospitalProfile hospitalProfile = new HospitalProfile();
        hospitalProfile.setUser(hospitalUser);
        hospitalProfile.setHospitalName("Metropolitan General Hospital");
        hospitalProfile.setRegistrationNumber("REG-ACC-32");
        hospitalProfile.setAddress("500 Emergency Blvd");
        hospitalProfile.setCity("Bangalore");
        hospitalProfile.setLatitude(12.9716);
        hospitalProfile.setLongitude(77.5946);
        hospitalProfile.setContactNumber("+91-8012345679");
        hospitalProfile = hospitalProfileRepository.save(hospitalProfile);

        EmergencyRequest request = new EmergencyRequest();
        request.setHospital(hospitalProfile);
        request.setBloodGroup("A+");
        request.setUnitsRequired(2);
        request.setUrgency(Urgency.CRITICAL);
        request.setStatus(RequestStatus.ACTIVE);
        request.setCurrentRadiusKm(20.0);
        request.setUnitsFulfilled(0);
        request.setCreatedAt(LocalDateTime.now());
        request.setExpiresAt(LocalDateTime.now().plusHours(2));
        request = emergencyRequestRepository.save(request);

        // 2. Arrange: Seed 32 Donors with MATCHED records and generate their tokens
        final int THREAD_COUNT = 32;
        record DonorSession(Long matchId, String token) {}
        List<DonorSession> sessions = new ArrayList<>(THREAD_COUNT);

        for (int i = 1; i <= THREAD_COUNT; i++) {
            User donorUser = new User();
            donorUser.setName("Accept Donor " + i);
            donorUser.setEmail("accept_donor_" + i + "@lifelink.com");
            donorUser.setPassword("$2a$10$dummyhashedpasswordfortestingpurposes");
            donorUser.setRole(Role.DONOR);
            donorUser = userRepository.save(donorUser);

            DonorProfile donorProfile = new DonorProfile();
            donorProfile.setUser(donorUser);
            donorProfile.setBloodGroup("A+");
            donorProfile.setAvailable(true);
            donorProfile.setAddress(i + " Donor Ave");
            donorProfile.setCity("Bangalore");
            donorProfile.setLatitude(12.9725);
            donorProfile.setLongitude(77.5955);
            donorProfile.setContactNumber("+91-98765431" + String.format("%02d", i));
            donorProfile = donorProfileRepository.save(donorProfile);

            RequestDonorMatch match = new RequestDonorMatch();
            match.setEmergencyRequest(request);
            match.setDonor(donorProfile);
            match.setStatus(MatchStatus.MATCHED);
            match.setCreatedAt(LocalDateTime.now());
            match.setContactSharingConsent(false);
            match = requestDonorMatchRepository.save(match);

            String token = jwtService.generateToken(donorUser.getId(), "DONOR");
            sessions.add(new DonorSession(match.getId(), token));
        }

        // 3. Act: 32 simultaneous Accept requests across 32 threads
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<Integer>> futures = new ArrayList<>(THREAD_COUNT);

        MatchStatusRequest acceptPayload = new MatchStatusRequest();
        acceptPayload.setStatus(MatchStatus.ACCEPTED);
        acceptPayload.setConsentToShareContact(true);
        String requestJson = objectMapper.writeValueAsString(acceptPayload);

        for (DonorSession session : sessions) {
            futures.add(pool.submit(() -> {
                readyLatch.countDown();
                startLatch.await(); // Synchronize all 32 threads to fire at once

                return mockMvc.perform(patch("/api/emergency-requests/matches/" + session.matchId() + "/status")
                                .header("Authorization", "Bearer " + session.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            }));
        }

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "All 32 threads must be ready within 10s");
        startLatch.countDown(); // Release all threads

        // 4. Gather status results
        AtomicInteger okCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherCount = new AtomicInteger(0);

        for (Future<Integer> future : futures) {
            int status = future.get(30, TimeUnit.SECONDS);
            if (status == 200) {
                okCount.incrementAndGet();
            } else if (status == 409) {
                conflictCount.incrementAndGet();
            } else {
                otherCount.incrementAndGet();
            }
        }

        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        // 5. Assertions
        assertEquals(2, okCount.get(), "Exactly 2 donor accepts must succeed for unitsRequired=2");
        assertEquals(30, conflictCount.get(), "Remaining 30 attempts must receive HTTP 409 Conflict");
        assertEquals(0, otherCount.get(), "No other HTTP status codes should be returned");

        long actualAccepted = requestDonorMatchRepository.countByEmergencyRequestAndStatus(request, MatchStatus.ACCEPTED);
        assertEquals(2, actualAccepted, "Exactly 2 match records in the database must be in ACCEPTED status");

        long actualMatched = requestDonorMatchRepository.countByEmergencyRequestAndStatus(request, MatchStatus.MATCHED);
        assertEquals(30, actualMatched, "Remaining 30 match records must remain in MATCHED status");
    }
}
