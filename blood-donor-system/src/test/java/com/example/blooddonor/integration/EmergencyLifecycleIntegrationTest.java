package com.example.blooddonor.integration;

import com.example.blooddonor.dto.*;
import com.example.blooddonor.entity.MatchStatus;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.entity.Urgency;
import com.example.blooddonor.repository.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmergencyLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

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
    @DisplayName("Complete Emergency Lifecycle: Hospital Request -> ABO/Haversine Matching -> Privacy Masking -> Donor Consent -> Contact Reveal -> Fulfillment")
    void completeEmergencyLifecycle_endToEndFlow() throws Exception {

        // ====================================================================
        // STEP 1: Register and Authenticate Hospital ("City Care Hospital")
        // ====================================================================
        RegisterRequest hospReg = new RegisterRequest();
        hospReg.setName("City Care Hospital");
        hospReg.setEmail("hospital@lifelink.com");
        hospReg.setPassword("Hospital123");
        hospReg.setRole("HOSPITAL");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hospReg)))
                .andExpect(status().isOk());

        LoginRequest hospLogin = new LoginRequest();
        hospLogin.setEmail("hospital@lifelink.com");
        hospLogin.setPassword("Hospital123");

        MvcResult hospLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hospLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String hospToken = JsonPath.read(hospLoginResult.getResponse().getContentAsString(), "$.token");

        // Create Hospital Profile (Coordinates: Bangalore Central 12.9352, 77.6245)
        HospitalProfileRequest hospProfile = new HospitalProfileRequest();
        hospProfile.setHospitalName("City Care Hospital");
        hospProfile.setRegistrationNumber("HOSP-BLR-001");
        hospProfile.setAddress("100 Hospital Boulevard");
        hospProfile.setCity("Bangalore");
        hospProfile.setLatitude(12.9352);
        hospProfile.setLongitude(77.6245);
        hospProfile.setContactNumber("+91-8012345678");

        mockMvc.perform(post("/api/hospitals/profile")
                        .header("Authorization", "Bearer " + hospToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hospProfile)))
                .andExpect(status().isOk());

        // ====================================================================
        // STEP 2: Register 3 Candidate Donors to Test Matching Matrix
        // ====================================================================

        // Donor 1: Alice (A+, ~4.8 km away -> COMPATIBLE & NEARBY -> MUST MATCH)
        String aliceToken = registerAndGetToken("Alice Donor", "alice@lifelink.com", "Donor123");
        createDonorProfile(aliceToken, "A+", 12.9716, 77.5946, "+91-9876543210", true);

        // Donor 2: Bob (B+, ~4.8 km away -> INCOMPATIBLE BLOOD TYPE -> MUST BE EXCLUDED)
        String bobToken = registerAndGetToken("Bob Donor", "bob@lifelink.com", "Donor123");
        createDonorProfile(bobToken, "B+", 12.9716, 77.5946, "+91-9876543211", true);

        // Donor 3: Charlie (A+, ~60 km away -> COMPATIBLE BUT > 20 KM -> MUST BE EXCLUDED)
        String charlieToken = registerAndGetToken("Charlie Donor", "charlie@lifelink.com", "Donor123");
        createDonorProfile(charlieToken, "A+", 13.3400, 77.1000, "+91-9876543212", true);

        // ====================================================================
        // STEP 3: Hospital Creates Emergency Request for A+ Blood
        // ====================================================================
        EmergencyRequestRequest bloodRequest = new EmergencyRequestRequest();
        bloodRequest.setBloodGroup("A+");
        bloodRequest.setUnitsRequired(1);
        bloodRequest.setUrgency(Urgency.CRITICAL);

        MvcResult reqResult = mockMvc.perform(post("/api/emergency-requests")
                        .header("Authorization", "Bearer " + hospToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bloodRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloodGroup").value("A+"))
                .andExpect(jsonPath("$.unitsRequired").value(1))
                .andExpect(jsonPath("$.urgency").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        Number reqIdNum = JsonPath.read(reqResult.getResponse().getContentAsString(), "$.id");
        Long requestId = reqIdNum.longValue();

        // ====================================================================
        // STEP 4: Trigger Matching Engine & Verify Haversine / ABO Matrix
        // ====================================================================
        MvcResult matchResult = mockMvc.perform(post("/api/emergency-requests/" + requestId + "/match")
                        .header("Authorization", "Bearer " + hospToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // Only Alice matches!
                .andExpect(jsonPath("$[0].donorName").value("Alice Donor"))
                .andExpect(jsonPath("$[0].bloodGroup").value("A+"))
                .andExpect(jsonPath("$[0].status").value("MATCHED"))
                .andReturn();

        Number matchIdNum = JsonPath.read(matchResult.getResponse().getContentAsString(), "$[0].id");
        Long matchId = matchIdNum.longValue();

        // Verify WebSocket notification was pushed to matching donor
        verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(
                anyString(),
                eq("/queue/notifications"),
                any()
        );

        // ====================================================================
        // STEP 5: Privacy Defense - Hospital Attempts to View Contact Before Consent
        // ====================================================================
        mockMvc.perform(get("/api/emergency-requests/" + requestId + "/matches/" + matchId + "/contact")
                        .header("Authorization", "Bearer " + hospToken))
                .andExpect(status().isConflict()) // 409 Conflict: BusinessRuleException
                .andExpect(jsonPath("$.message").value(containsString("Contact details are only available for accepted donor matches")));

        // ====================================================================
        // STEP 6: Donor Alice Accepts Match with Contact Sharing Consent
        // ====================================================================
        MatchStatusRequest acceptRequest = new MatchStatusRequest();
        acceptRequest.setStatus(MatchStatus.ACCEPTED);
        acceptRequest.setConsentToShareContact(true);

        mockMvc.perform(patch("/api/emergency-requests/matches/" + matchId + "/status")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // ====================================================================
        // STEP 7: Privacy Unlocked - Hospital Successfully Fetches Donor Contact
        // ====================================================================
        mockMvc.perform(get("/api/emergency-requests/" + requestId + "/matches/" + matchId + "/contact")
                        .header("Authorization", "Bearer " + hospToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorName").value("Alice Donor"))
                .andExpect(jsonPath("$.bloodGroup").value("A+"))
                .andExpect(jsonPath("$.contactNumber").value("+91-9876543210"))
                .andExpect(jsonPath("$.email").value("alice@lifelink.com"))
                .andExpect(jsonPath("$.contactSharingConsent").value(true))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // ====================================================================
        // STEP 8: Hospital Marks Match Completed -> Request Automatically Fulfilled
        // ====================================================================
        mockMvc.perform(patch("/api/emergency-requests/matches/" + matchId + "/complete")
                        .header("Authorization", "Bearer " + hospToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Verify Emergency Request was automatically transitioned to FULFILLED
        mockMvc.perform(get("/api/emergency-requests")
                        .header("Authorization", "Bearer " + hospToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("FULFILLED"));
    }

    // --- Helper Methods ---

    private String registerAndGetToken(String name, String email, String password) throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setName(name);
        reg.setEmail(email);
        reg.setPassword(password);
        reg.setRole("DONOR");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword(password);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");
    }

    private void createDonorProfile(String token, String bloodGroup, double lat, double lon, String contact, boolean available) throws Exception {
        DonorProfileRequest profile = new DonorProfileRequest();
        profile.setBloodGroup(bloodGroup);
        profile.setAddress("12 Test Street");
        profile.setCity("Bangalore");
        profile.setLatitude(lat);
        profile.setLongitude(lon);
        profile.setContactNumber(contact);
        profile.setAvailable(available);

        mockMvc.perform(post("/api/donors/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk());
    }
}

