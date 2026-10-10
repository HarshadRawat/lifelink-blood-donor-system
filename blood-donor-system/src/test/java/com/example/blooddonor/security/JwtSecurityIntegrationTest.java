package com.example.blooddonor.security;
import com.example.blooddonor.dto.RequestDonorMatchResponse;

import java.util.List;
import com.example.blooddonor.dto.RequestDonorMatchResponse;
import com.example.blooddonor.entity.MatchStatus;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.blooddonor.config.SecurityConfig;
import com.example.blooddonor.controller.DonorProfileController;
import com.example.blooddonor.controller.HospitalProfileController;
import com.example.blooddonor.dto.DonorProfileResponse;
import com.example.blooddonor.dto.HospitalProfileResponse;
import com.example.blooddonor.service.DonorMatchingService;
import com.example.blooddonor.service.DonorProfileService;
import com.example.blooddonor.service.HospitalProfileService;
import com.example.blooddonor.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        DonorProfileController.class,
        HospitalProfileController.class
})
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtFilter.class,
        JwtService.class
})
class JwtSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DonorProfileService donorProfileService;

    @MockitoBean
    private DonorMatchingService donorMatchingService;

    @MockitoBean
    private HospitalProfileService hospitalProfileService;


    // ---------------------------------------------------------
    // Real DONOR JWT → DONOR endpoint
    // ---------------------------------------------------------

    @Test
    void donorJwtCanAccessDonorEndpoint() throws Exception {

        String token = jwtService.generateToken(9L, "DONOR");

        DonorProfileResponse response = new DonorProfileResponse();

        when(donorProfileService.getProfile(9L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/donors/profile")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk());
    }


    // ---------------------------------------------------------
    // Real HOSPITAL JWT → HOSPITAL endpoint
    // ---------------------------------------------------------

    @Test
    void hospitalJwtCanAccessHospitalEndpoint() throws Exception {

        String token = jwtService.generateToken(8L, "HOSPITAL");

        HospitalProfileResponse response =
                new HospitalProfileResponse();

        when(hospitalProfileService.getProfile(8L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/hospitals/profile")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk());
    }


    // ---------------------------------------------------------
    // Real DONOR JWT → HOSPITAL endpoint
    // ---------------------------------------------------------

    @Test
    void donorJwtCannotAccessHospitalEndpoint()
            throws Exception {

        String token =
                jwtService.generateToken(9L, "DONOR");

        mockMvc.perform(
                        get("/api/hospitals/profile")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isForbidden());
    }


    // ---------------------------------------------------------
    // Real HOSPITAL JWT → DONOR endpoint
    // ---------------------------------------------------------

    @Test
    void hospitalJwtCannotAccessDonorEndpoint()
            throws Exception {

        String token =
                jwtService.generateToken(8L, "HOSPITAL");

        mockMvc.perform(
                        get("/api/donors/profile")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isForbidden());
    }


    // ---------------------------------------------------------
    // Invalid JWT
    // ---------------------------------------------------------

    @Test
    void invalidJwtReturnsUnauthorized() throws Exception {

        mockMvc.perform(
                        get("/api/donors/profile")
                                .header(
                                        "Authorization",
                                        "Bearer invalid-token"
                                )
                )
                .andExpect(status().isUnauthorized());
    }

}