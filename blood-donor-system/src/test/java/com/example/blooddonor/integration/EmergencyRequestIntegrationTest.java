package com.example.blooddonor.integration;

import com.example.blooddonor.config.SecurityConfig;
import com.example.blooddonor.controller.EmergencyRequestController;
import com.example.blooddonor.dto.EmergencyRequestRequest;
import com.example.blooddonor.dto.EmergencyRequestResponse;
import com.example.blooddonor.dto.RequestDonorMatchResponse;
import com.example.blooddonor.entity.MatchStatus;
import com.example.blooddonor.entity.Urgency;
import com.example.blooddonor.security.JwtFilter;
import com.example.blooddonor.service.DonorMatchingService;
import com.example.blooddonor.service.EmergencyRequestService;
import com.example.blooddonor.service.JwtService;
import com.example.blooddonor.dto.MatchStatusRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(EmergencyRequestController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtFilter.class,
        JwtService.class
})
class EmergencyRequestIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtService jwtService;

    @MockitoBean
    EmergencyRequestService emergencyRequestService;

    @MockitoBean
    DonorMatchingService donorMatchingService;


    @Test
    void hospitalCanCreateEmergencyRequestUsingJwt()
            throws Exception {

        String token =
                jwtService.generateToken(8L, "HOSPITAL");

        EmergencyRequestResponse response =
                new EmergencyRequestResponse();

        response.setId(1L);
        response.setBloodGroup("O+");
        response.setUnitsRequired(2);
        response.setUrgency(Urgency.HIGH);

        when(
                emergencyRequestService.createRequest(
                        any(EmergencyRequestRequest.class),
                        eq(8L)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        post("/api/emergency-requests")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType("application/json")
                                .content("""
                                        {
                                            "bloodGroup": "O+",
                                            "unitsRequired": 2,
                                            "urgency": "HIGH"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id").value(1)
                )
                .andExpect(
                        jsonPath("$.bloodGroup").value("O+")
                )
                .andExpect(
                        jsonPath("$.unitsRequired").value(2)
                )
                .andExpect(
                        jsonPath("$.urgency").value("HIGH")
                );

        verify(
                emergencyRequestService
        ).createRequest(
                any(EmergencyRequestRequest.class),
                eq(8L)
        );
    }


    @Test
    void donorCannotCreateEmergencyRequest()
            throws Exception {

        String token =
                jwtService.generateToken(9L, "DONOR");

        mockMvc.perform(
                        post("/api/emergency-requests")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType("application/json")
                                .content("""
                                        {
                                            "bloodGroup": "O+",
                                            "unitsRequired": 2,
                                            "urgency": "HIGH"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        verify(
                emergencyRequestService,
                never()
        ).createRequest(
                any(EmergencyRequestRequest.class),
                anyLong()
        );
    }


    @Test
    void hospitalCanGetEmergencyRequestsUsingJwt()
            throws Exception {

        String token =
                jwtService.generateToken(8L, "HOSPITAL");

        EmergencyRequestResponse response =
                new EmergencyRequestResponse();

        response.setId(1L);
        response.setBloodGroup("O+");
        response.setUnitsRequired(2);
        response.setUrgency(Urgency.HIGH);

        Page<EmergencyRequestResponse> page =
                new PageImpl<>(
                        List.of(response)
                );

        when(
                emergencyRequestService.getMyRequests(
                        eq(8L),
                        isNull(),
                        isNull(),
                        isNull(),
                        any(Pageable.class)
                )
        ).thenReturn(page);

        mockMvc.perform(
                        get("/api/emergency-requests")
                                .param("page", "0")
                                .param("size", "2")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content[0].id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].bloodGroup")
                                .value("O+")
                )
                .andExpect(
                        jsonPath("$.content[0].unitsRequired")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.content[0].urgency")
                                .value("HIGH")
                );

        verify(
                emergencyRequestService
        ).getMyRequests(
                eq(8L),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }


    @Test
    void hospitalCanFindMatchingDonorsUsingJwt()
            throws Exception {

        String token =
                jwtService.generateToken(8L, "HOSPITAL");

        RequestDonorMatchResponse match =
                new RequestDonorMatchResponse();

        match.setId(1L);
        match.setEmergencyRequestId(1L);
        match.setDonorId(1L);
        match.setDonorName("Test Donor");
        match.setBloodGroup("O+");
        match.setStatus(MatchStatus.MATCHED);

        when(
                donorMatchingService.findMatches(1L)
        ).thenReturn(
                List.of(match)
        );

        mockMvc.perform(
                        post(
                                "/api/emergency-requests/1/match"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].emergencyRequestId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].donorId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].donorName")
                                .value("Test Donor")
                )
                .andExpect(
                        jsonPath("$[0].bloodGroup")
                                .value("O+")
                )
                .andExpect(
                        jsonPath("$[0].status")
                                .value("MATCHED")
                );

        verify(
                donorMatchingService
        ).findMatches(1L);
    }

    @Test
    void donorCanAcceptMatchUsingJwt()
            throws Exception {

        // Create a real JWT for donor user ID 9
        String token =
                jwtService.generateToken(9L, "DONOR");

        // Prepare the response returned by the service
        RequestDonorMatchResponse response =
                new RequestDonorMatchResponse();

        response.setId(1L);
        response.setEmergencyRequestId(1L);
        response.setDonorId(1L);
        response.setDonorName("Test Donor");
        response.setBloodGroup("O+");
        response.setStatus(MatchStatus.ACCEPTED);

        // Service returns the accepted match
        when(
                donorMatchingService.updateMatchStatus(
                        eq(1L),
                        any(MatchStatusRequest.class),
                        eq(9L)
                )
        ).thenReturn(response);

        // Donor accepts the match
        mockMvc.perform(
                        patch(
                                "/api/emergency-requests/matches/1/status"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType("application/json")
                                .content("""
                                    {
                                        "status": "ACCEPTED"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id").value(1)
                )
                .andExpect(
                        jsonPath("$.emergencyRequestId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.donorId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.donorName")
                                .value("Test Donor")
                )
                .andExpect(
                        jsonPath("$.bloodGroup")
                                .value("O+")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACCEPTED")
                );

        // Verify the correct match ID and donor user ID reached the service
        verify(
                donorMatchingService
        ).updateMatchStatus(
                eq(1L),
                any(MatchStatusRequest.class),
                eq(9L)
        );
    }

    @Test
    void hospitalCanCompleteAcceptedMatchUsingJwt()
            throws Exception {

        // Create a real JWT for hospital user ID 8
        String token =
                jwtService.generateToken(8L, "HOSPITAL");

        // Prepare the response returned by the service
        RequestDonorMatchResponse response =
                new RequestDonorMatchResponse();

        response.setId(1L);
        response.setEmergencyRequestId(1L);
        response.setDonorId(1L);
        response.setDonorName("Test Donor");
        response.setBloodGroup("O+");
        response.setStatus(MatchStatus.COMPLETED);

        // Mock service response
        when(
                donorMatchingService.completeMatch(
                        eq(1L),
                        eq(8L)
                )
        ).thenReturn(response);

        // Hospital completes the accepted match
        mockMvc.perform(
                        patch(
                                "/api/emergency-requests/matches/1/complete"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id").value(1)
                )
                .andExpect(
                        jsonPath("$.emergencyRequestId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.donorId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.donorName")
                                .value("Test Donor")
                )
                .andExpect(
                        jsonPath("$.bloodGroup")
                                .value("O+")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("COMPLETED")
                );

        // Verify hospital user ID reached the service
        verify(
                donorMatchingService
        ).completeMatch(
                eq(1L),
                eq(8L)
        );
    }
}