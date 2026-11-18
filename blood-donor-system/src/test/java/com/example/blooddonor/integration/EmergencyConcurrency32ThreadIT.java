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
 * Task 8: 32-Thread Concurrency Test on Testcontainers MySQL.
 *
 * Verifies that under extreme concurrency (32 threads):
 * 1. Exactly the required number of units (unitsRequired = 2) are completed (HTTP 200).
 * 2. Excess simultaneous completion attempts are safely rejected with HTTP 409 Conflict.
 * 3. Database state remains 100% consistent: request transitions to FULFILLED, completed units never exceed required units.
 * 4. Concurrent donor acceptance attempts on a fulfilled request return HTTP 409 Conflict.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EmergencyConcurrency32ThreadIT extends AbstractMySqlIT {

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
    @DisplayName("32-Thread Concurrency: 32 concurrent match completions for a 2-unit request must yield exactly 2 OKs (200) and 30 Conflicts (409)")
    void concurrentCompletions_mustNeverExceedUnitsRequiredAndExcessMustReturn409() throws Exception {
        // 1. Arrange: Create Hospital User and Profile
        User hospitalUser = new User();
        hospitalUser.setName("Central City Hospital");
        hospitalUser.setEmail("hospital_concurrency@lifelink.com");
        hospitalUser.setPassword("$2a$10$dummyhashedpasswordfortestingpurposes");
        hospitalUser.setRole(Role.HOSPITAL);
        hospitalUser = userRepository.save(hospitalUser);

        HospitalProfile hospitalProfile = new HospitalProfile();
        hospitalProfile.setUser(hospitalUser);
        hospitalProfile.setHospitalName("Central City Hospital");
        hospitalProfile.setRegistrationNumber("REG-CONCUR-32");
        hospitalProfile.setAddress("400 Concurrency Ave");
        hospitalProfile.setCity("Bangalore");
        hospitalProfile.setLatitude(12.9716);
        hospitalProfile.setLongitude(77.5946);
        hospitalProfile.setContactNumber("+91-8012345678");
        hospitalProfile = hospitalProfileRepository.save(hospitalProfile);

        String hospitalToken = jwtService.generateToken(hospitalUser.getId(), "HOSPITAL");

        // 2. Arrange: Create EmergencyRequest requiring 2 units
        EmergencyRequest request = new EmergencyRequest();
        request.setHospital(hospitalProfile);
        request.setBloodGroup("O+");
        request.setUnitsRequired(2);
        request.setUrgency(Urgency.CRITICAL);
        request.setStatus(RequestStatus.ACTIVE);
        request.setCurrentRadiusKm(20.0);
        request.setUnitsFulfilled(0);
        request.setCreatedAt(LocalDateTime.now());
        request = emergencyRequestRepository.save(request);

        // 3. Arrange: Seed 32 Donors with ACCEPTED matches
        final int THREAD_COUNT = 32;
        List<Long> matchIds = new ArrayList<>(THREAD_COUNT);

        for (int i = 1; i <= THREAD_COUNT; i++) {
            User donorUser = new User();
            donorUser.setName("Donor Thread " + i);
            donorUser.setEmail("donor_thread_" + i + "@lifelink.com");
            donorUser.setPassword("$2a$10$dummyhashedpasswordfortestingpurposes");
            donorUser.setRole(Role.DONOR);
            donorUser = userRepository.save(donorUser);

            DonorProfile donorProfile = new DonorProfile();
            donorProfile.setUser(donorUser);
            donorProfile.setBloodGroup("O+");
            donorProfile.setAvailable(true);
            donorProfile.setAddress(i + " Runner Road");
            donorProfile.setCity("Bangalore");
            donorProfile.setLatitude(12.9720);
            donorProfile.setLongitude(77.5950);
            donorProfile.setContactNumber("+91-98765432" + String.format("%02d", i));
            donorProfile = donorProfileRepository.save(donorProfile);

            RequestDonorMatch match = new RequestDonorMatch();
            match.setEmergencyRequest(request);
            match.setDonor(donorProfile);
            match.setStatus(MatchStatus.ACCEPTED);
            match.setCreatedAt(LocalDateTime.now());
            match.setContactSharingConsent(true);
            match = requestDonorMatchRepository.save(match);

            matchIds.add(match.getId());
        }

        // 4. Act: Launch 32 simultaneous completion requests across 32 threads
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<Integer>> futures = new ArrayList<>(THREAD_COUNT);

        for (Long matchId : matchIds) {
            futures.add(pool.submit(() -> {
                readyLatch.countDown();
                startLatch.await(); // Synchronize all 32 threads to fire at the exact same instant

                return mockMvc.perform(patch("/api/emergency-requests/matches/" + matchId + "/complete")
                                .header("Authorization", "Bearer " + hospitalToken))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            }));
        }

        // Wait until all 32 worker threads are ready at the starting gate
        assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "All 32 threads must be ready within 10s");
        startLatch.countDown(); // BANG: release all threads simultaneously

        // 5. Gather and tally HTTP status codes
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

        // 6. Assertions on HTTP responses
        assertEquals(2, okCount.get(), "Exactly 2 completions must succeed for unitsRequired=2");
        assertEquals(30, conflictCount.get(), "Remaining 30 attempts must receive HTTP 409 Conflict");
        assertEquals(0, otherCount.get(), "No other HTTP status codes should be returned");

        // 7. Assertions on Database consistency
        EmergencyRequest updatedRequest = emergencyRequestRepository.findById(request.getId()).orElseThrow();
        assertEquals(RequestStatus.FULFILLED, updatedRequest.getStatus(), "Request must be marked FULFILLED");

        long actualCompletedUnits = requestDonorMatchRepository.countByEmergencyRequestAndStatus(
                updatedRequest,
                MatchStatus.COMPLETED
        );
        assertEquals(2L, actualCompletedUnits, "Completed units in DB must strictly equal unitsRequired (2)");
    }

    @Test
    @DisplayName("Concurrent Donor Acceptance: Donors attempting to accept an already-fulfilled request must receive HTTP 409 Conflict")
    void concurrentDonorAcceptance_whenRequestFulfilled_mustRejectWith409() throws Exception {
        // 1. Arrange: Create Hospital and Fulfilled Emergency Request
        User hospitalUser = new User();
        hospitalUser.setName("Apex Hospital");
        hospitalUser.setEmail("apex_hosp@lifelink.com");
        hospitalUser.setPassword("$2a$10$dummyhashedpasswordfortestingpurposes");
        hospitalUser.setRole(Role.HOSPITAL);
        hospitalUser = userRepository.save(hospitalUser);

        HospitalProfile hospitalProfile = new HospitalProfile();
        hospitalProfile.setUser(hospitalUser);
        hospitalProfile.setHospitalName("Apex Hospital");
        hospitalProfile.setRegistrationNumber("REG-APEX-01");
        hospitalProfile.setAddress("500 Apex Way");
        hospitalProfile.setCity("Bangalore");
        hospitalProfile.setLatitude(12.9716);
        hospitalProfile.setLongitude(77.5946);
        hospitalProfile.setContactNumber("+91-8012345679");
        hospitalProfile = hospitalProfileRepository.save(hospitalProfile);

        EmergencyRequest request = new EmergencyRequest();
        request.setHospital(hospitalProfile);
        request.setBloodGroup("B+");
        request.setUnitsRequired(1);
        request.setUrgency(Urgency.HIGH);
        request.setStatus(RequestStatus.FULFILLED); // Request already fulfilled
        request.setCurrentRadiusKm(20.0);
        request.setCreatedAt(LocalDateTime.now().minusHours(2));
        request = emergencyRequestRepository.save(request);

        // 2. Arrange: 20 Donors with MATCHED status trying to accept
        final int THREAD_COUNT = 20;
        List<Long> matchIds = new ArrayList<>(THREAD_COUNT);
        List<String> donorTokens = new ArrayList<>(THREAD_COUNT);

        for (int i = 1; i <= THREAD_COUNT; i++) {
            User donorUser = new User();
            donorUser.setName("Late Donor " + i);
            donorUser.setEmail("late_donor_" + i + "@lifelink.com");
            donorUser.setPassword("$2a$10$dummyhashedpasswordfortestingpurposes");
            donorUser.setRole(Role.DONOR);
            donorUser = userRepository.save(donorUser);

            DonorProfile donorProfile = new DonorProfile();
            donorProfile.setUser(donorUser);
            donorProfile.setBloodGroup("B+");
            donorProfile.setAvailable(true);
            donorProfile.setAddress(i + " Late Lane");
            donorProfile.setCity("Bangalore");
            donorProfile.setLatitude(12.9720);
            donorProfile.setLongitude(77.5950);
            donorProfile.setContactNumber("+91-98765433" + String.format("%02d", i));
            donorProfile = donorProfileRepository.save(donorProfile);

            RequestDonorMatch match = new RequestDonorMatch();
            match.setEmergencyRequest(request);
            match.setDonor(donorProfile);
            match.setStatus(MatchStatus.MATCHED);
            match.setCreatedAt(LocalDateTime.now());
            match = requestDonorMatchRepository.save(match);

            matchIds.add(match.getId());
            donorTokens.add(jwtService.generateToken(donorUser.getId(), "DONOR"));
        }

        // 3. Act: 20 threads simultaneously attempt to accept the fulfilled request
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<Integer>> futures = new ArrayList<>(THREAD_COUNT);

        MatchStatusRequest acceptReq = new MatchStatusRequest();
        acceptReq.setStatus(MatchStatus.ACCEPTED);
        acceptReq.setConsentToShareContact(true);
        String jsonPayload = objectMapper.writeValueAsString(acceptReq);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final Long mId = matchIds.get(i);
            final String token = donorTokens.get(i);

            futures.add(pool.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                return mockMvc.perform(patch("/api/emergency-requests/matches/" + mId + "/status")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonPayload))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            }));
        }

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        // 4. Assert: All 20 threads must receive HTTP 409 Conflict
        AtomicInteger conflictCount = new AtomicInteger(0);
        for (Future<Integer> future : futures) {
            int status = future.get(30, TimeUnit.SECONDS);
            if (status == 409) {
                conflictCount.incrementAndGet();
            }
        }

        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(20, conflictCount.get(), "All 20 donor accept attempts on fulfilled request must return 409 Conflict");
        assertEquals(0L, requestDonorMatchRepository.countByEmergencyRequestAndStatus(request, MatchStatus.ACCEPTED),
                "Zero donors should be in ACCEPTED status");
    }
}
