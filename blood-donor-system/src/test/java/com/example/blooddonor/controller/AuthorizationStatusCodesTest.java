package com.example.blooddonor.controller;

import com.example.blooddonor.dto.EmergencyRequestStatusRequest;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.exception.ForbiddenOperationException;
import com.example.blooddonor.exception.ResourceNotFoundException;
import com.example.blooddonor.service.DonorMatchingService;
import com.example.blooddonor.service.EmergencyRequestService;
import com.example.blooddonor.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmergencyRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Authorization and Resource HTTP Status Code Mapping Tests")
class AuthorizationStatusCodesTest {

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

    @Test
    @DisplayName("Hospital attempting to modify another hospital's request returns HTTP 403 Forbidden")
    void whenUnauthorizedHospitalUpdatesRequest_shouldReturn403() throws Exception {
        EmergencyRequestStatusRequest request = new EmergencyRequestStatusRequest();
        request.setStatus(RequestStatus.CANCELLED);

        when(emergencyRequestService.updateStatus(eq(100L), any(), any()))
                .thenThrow(new ForbiddenOperationException("You are not allowed to update this request"));

        mockMvc.perform(patch("/api/emergency-requests/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(mock(Authentication.class)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to update this request"));
    }

    @Test
    @DisplayName("Requesting non-existent emergency request id returns HTTP 404 Not Found")
    void whenEmergencyRequestNotFound_shouldReturn404() throws Exception {
        EmergencyRequestStatusRequest request = new EmergencyRequestStatusRequest();
        request.setStatus(RequestStatus.CANCELLED);

        when(emergencyRequestService.updateStatus(eq(999L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Emergency request not found"));

        mockMvc.perform(patch("/api/emergency-requests/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(mock(Authentication.class)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Emergency request not found"));
    }

    @Test
    @DisplayName("Unauthorized attempt to access candidate matches for another hospital returns HTTP 403")
    void whenUnauthorizedHospitalQueriesMatches_shouldReturn403() throws Exception {
        when(donorMatchingService.getRequestMatches(eq(100L), any()))
                .thenThrow(new ForbiddenOperationException("You are not allowed to view this request"));

        mockMvc.perform(get("/api/emergency-requests/100/matches")
                        .principal(mock(Authentication.class)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to view this request"));
    }

    @Test
    @DisplayName("Querying candidate matches for non-existent emergency request returns HTTP 404")
    void whenQueryingMatchesForMissingRequest_shouldReturn404() throws Exception {
        when(donorMatchingService.getRequestMatches(eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Emergency request not found"));

        mockMvc.perform(get("/api/emergency-requests/999/matches")
                        .principal(mock(Authentication.class)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Emergency request not found"));
    }
}
