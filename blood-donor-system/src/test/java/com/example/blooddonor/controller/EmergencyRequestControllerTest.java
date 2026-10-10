package com.example.blooddonor.controller;

import com.example.blooddonor.dto.EmergencyRequestRequest;
import com.example.blooddonor.dto.EmergencyRequestResponse;
import com.example.blooddonor.dto.EmergencyRequestStatusRequest;
import com.example.blooddonor.dto.MatchStatusRequest;
import com.example.blooddonor.dto.DonorContactResponse;
import com.example.blooddonor.dto.RequestDonorMatchResponse;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.entity.Urgency;
import com.example.blooddonor.service.DonorMatchingService;
import com.example.blooddonor.service.EmergencyRequestService;
import com.example.blooddonor.service.JwtService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(EmergencyRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmergencyRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private EmergencyRequestService emergencyRequestService;

    @MockitoBean
    private DonorMatchingService donorMatchingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private Authentication authentication;


    // =========================================================
    // 1. CREATE EMERGENCY REQUEST - SUCCESS
    // =========================================================

    @Test
    void createRequest_shouldReturn200_whenValidRequest()
            throws Exception {

        EmergencyRequestRequest request =
                new EmergencyRequestRequest();

        request.setBloodGroup("O+");
        request.setUnitsRequired(2);
        request.setUrgency(Urgency.HIGH);


        EmergencyRequestResponse response =
                new EmergencyRequestResponse();

        response.setId(1L);
        response.setBloodGroup("O+");
        response.setUnitsRequired(2);
        response.setUrgency(Urgency.HIGH);
        response.setStatus(RequestStatus.ACTIVE);


        when(authentication.getPrincipal())
                .thenReturn(8L);

        when(emergencyRequestService.createRequest(
                any(EmergencyRequestRequest.class),
                eq(8L)
        )).thenReturn(response);


        mockMvc.perform(
                        post("/api/emergency-requests")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bloodGroup").value("O+"))
                .andExpect(jsonPath("$.unitsRequired").value(2))
                .andExpect(jsonPath("$.urgency").value("HIGH"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }


    // =========================================================
    // 2. CREATE EMERGENCY REQUEST - VALIDATION FAILURE
    // =========================================================

    @Test
    void createRequest_shouldReturn400_whenInvalidRequest()
            throws Exception {

        EmergencyRequestRequest request =
                new EmergencyRequestRequest();

        request.setBloodGroup("");
        request.setUnitsRequired(0);
        request.setUrgency(null);


        mockMvc.perform(
                        post("/api/emergency-requests")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isBadRequest());
    }


    // =========================================================
    // 3. GET MY REQUESTS - SUCCESS
    // =========================================================

    @Test
    void getMyRequests_shouldReturn200_whenRequestsExist()
            throws Exception {

        EmergencyRequestResponse response =
                new EmergencyRequestResponse();

        response.setId(1L);
        response.setBloodGroup("O+");
        response.setUnitsRequired(2);
        response.setUrgency(Urgency.HIGH);
        response.setStatus(RequestStatus.ACTIVE);


        PageImpl<EmergencyRequestResponse> page =
                new PageImpl<>(
                        List.of(response),
                        PageRequest.of(0, 2),
                        1
                );


        when(authentication.getPrincipal())
                .thenReturn(8L);

        when(emergencyRequestService.getMyRequests(
                eq(8L),
                eq(RequestStatus.ACTIVE),
                eq("O+"),
                eq(Urgency.HIGH),
                any()
        )).thenReturn(page);


        mockMvc.perform(
                        get("/api/emergency-requests")
                                .principal(authentication)
                                .param("status", "ACTIVE")
                                .param("bloodGroup", "O+")
                                .param("urgency", "HIGH")
                                .param("page", "0")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].bloodGroup")
                        .value("O+"))
                .andExpect(jsonPath("$.content[0].unitsRequired")
                        .value(2))
                .andExpect(jsonPath("$.content[0].urgency")
                        .value("HIGH"))
                .andExpect(jsonPath("$.content[0].status")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.totalElements")
                        .value(1));
    }


    // =========================================================
    // 4. UPDATE REQUEST STATUS - SUCCESS
    // =========================================================

    @Test
    void updateStatus_shouldReturn200_whenValidRequest()
            throws Exception {

        EmergencyRequestStatusRequest request =
                new EmergencyRequestStatusRequest();

        request.setStatus(RequestStatus.FULFILLED);


        EmergencyRequestResponse response =
                new EmergencyRequestResponse();

        response.setId(1L);
        response.setBloodGroup("O+");
        response.setUnitsRequired(2);
        response.setUrgency(Urgency.HIGH);
        response.setStatus(RequestStatus.FULFILLED);


        when(authentication.getPrincipal())
                .thenReturn(8L);

        when(emergencyRequestService.updateStatus(
                eq(1L),
                any(EmergencyRequestStatusRequest.class),
                eq(8L)
        )).thenReturn(response);


        mockMvc.perform(
                        patch("/api/emergency-requests/1/status")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status")
                        .value("FULFILLED"));
    }


    // =========================================================
    // 5. FIND MATCHES - SUCCESS
    // =========================================================

    @Test
    void findMatches_shouldReturn200_whenMatchesFound()
            throws Exception {

        RequestDonorMatchResponse match =
                new RequestDonorMatchResponse();

        match.setId(1L);


        when(donorMatchingService.findMatches(1L))
                .thenReturn(List.of(match));


        mockMvc.perform(
                        post("/api/emergency-requests/1/match")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }


    // =========================================================
    // 6. UPDATE MATCH STATUS - SUCCESS
    // =========================================================

    @Test
    void updateMatchStatus_shouldReturn200_whenValidRequest()
            throws Exception {

        MatchStatusRequest request =
                new MatchStatusRequest();

        request.setStatus(com.example.blooddonor.entity.MatchStatus.ACCEPTED);


        RequestDonorMatchResponse response =
                new RequestDonorMatchResponse();

        response.setId(1L);


        when(authentication.getPrincipal())
                .thenReturn(9L);

        when(donorMatchingService.updateMatchStatus(
                eq(1L),
                any(MatchStatusRequest.class),
                eq(9L)
        )).thenReturn(response);


        mockMvc.perform(
                        patch("/api/emergency-requests/matches/1/status")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }


    // =========================================================
    // 7. COMPLETE MATCH - SUCCESS
    // =========================================================

    @Test
    void completeMatch_shouldReturn200_whenValidMatch()
            throws Exception {

        RequestDonorMatchResponse response =
                new RequestDonorMatchResponse();

        response.setId(1L);


        when(authentication.getPrincipal())
                .thenReturn(8L);

        when(donorMatchingService.completeMatch(
                eq(1L),
                eq(8L)
        )).thenReturn(response);


        mockMvc.perform(
                        patch("/api/emergency-requests/matches/1/complete")
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }


    // =========================================================
    // 8. GET REQUEST MATCHES - SUCCESS
    // =========================================================

    @Test
    void getRequestMatches_shouldReturn200_whenMatchesExist()
            throws Exception {

        RequestDonorMatchResponse match =
                new RequestDonorMatchResponse();

        match.setId(1L);


        when(authentication.getPrincipal())
                .thenReturn(8L);

        when(donorMatchingService.getRequestMatches(
                eq(1L),
                eq(8L)
        )).thenReturn(List.of(match));


        mockMvc.perform(
                        get("/api/emergency-requests/1/matches")
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }


    // =========================================================
    // 9. GET DONOR CONTACT - SUCCESS
    // =========================================================

    @Test
    void getDonorContact_shouldReturn200_whenAuthorizedAndAccepted()
            throws Exception {

        DonorContactResponse contact = DonorContactResponse.builder()
                .matchId(2L)
                .requestId(1L)
                .donorId(5L)
                .donorName("John Doe")
                .bloodGroup("O_POSITIVE")
                .contactNumber("9876543210")
                .email("john@donor.com")
                .status(com.example.blooddonor.entity.MatchStatus.ACCEPTED)
                .contactSharingConsent(true)
                .build();

        when(authentication.getPrincipal())
                .thenReturn(8L);

        when(donorMatchingService.getDonorContact(eq(1L), eq(2L), eq(8L)))
                .thenReturn(contact);

        mockMvc.perform(
                        get("/api/emergency-requests/1/matches/2/contact")
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorName").value("John Doe"))
                .andExpect(jsonPath("$.contactNumber").value("9876543210"))
                .andExpect(jsonPath("$.email").value("john@donor.com"))
                .andExpect(jsonPath("$.contactSharingConsent").value(true));
    }

    // =========================================================
    // 10. GET DONOR CONTACT - FORBIDDEN (SECURITY EXCEPTION)
    // =========================================================

    @Test
    void getDonorContact_shouldReturn403_whenUnauthorized()
            throws Exception {

        when(authentication.getPrincipal())
                .thenReturn(8L);

        when(donorMatchingService.getDonorContact(eq(1L), eq(2L), eq(8L)))
                .thenThrow(new SecurityException("Hospital not authorized"));

        mockMvc.perform(
                        get("/api/emergency-requests/1/matches/2/contact")
                                .principal(authentication)
                )
                .andExpect(status().isForbidden());
    }
}