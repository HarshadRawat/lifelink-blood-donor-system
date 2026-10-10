package com.example.blooddonor.security;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        DonorProfileController.class,
        HospitalProfileController.class
})
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtFilter.class
})
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DonorProfileService donorProfileService;

    @MockitoBean
    private DonorMatchingService donorMatchingService;

    @MockitoBean
    private HospitalProfileService hospitalProfileService;

    @MockitoBean
    private JwtService jwtService;


    // ---------------------------------------------------------
    // DONOR → DONOR endpoint
    // ---------------------------------------------------------

    @Test
    void donorCanAccessDonorEndpoint() throws Exception {

        DonorProfileResponse response =
                new DonorProfileResponse();

        when(donorProfileService.getProfile(anyLong()))
                .thenReturn(response);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        9L,
                        null,
                        List.of(() ->
                                "ROLE_DONOR")
                );

        mockMvc.perform(
                        get("/api/donors/profile")
                                .with(authentication(authentication))
                )
                .andExpect(status().isOk());
    }


    // ---------------------------------------------------------
    // HOSPITAL → HOSPITAL endpoint
    // ---------------------------------------------------------

    @Test
    void hospitalCanAccessHospitalEndpoint() throws Exception {

        HospitalProfileResponse response =
                new HospitalProfileResponse();

        when(hospitalProfileService.getProfile(anyLong()))
                .thenReturn(response);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        8L,
                        null,
                        List.of(() ->
                                "ROLE_HOSPITAL")
                );

        mockMvc.perform(
                        get("/api/hospitals/profile")
                                .with(authentication(authentication))
                )
                .andExpect(status().isOk());
    }


    // ---------------------------------------------------------
    // DONOR → HOSPITAL endpoint
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "DONOR")
    void donorCannotAccessHospitalEndpoint() throws Exception {

        mockMvc.perform(
                        get("/api/hospitals/profile")
                )
                .andExpect(status().isForbidden());
    }


    // ---------------------------------------------------------
    // HOSPITAL → DONOR endpoint
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "HOSPITAL")
    void hospitalCannotAccessDonorEndpoint() throws Exception {

        mockMvc.perform(
                        get("/api/donors/profile")
                )
                .andExpect(status().isForbidden());
    }


    // ---------------------------------------------------------
    // No authentication
    // ---------------------------------------------------------

    @Test
    void unauthenticatedUserCannotAccessDonorEndpoint()
            throws Exception {

        mockMvc.perform(
                        get("/api/donors/profile")
                )
                .andExpect(status().isUnauthorized());;
    }
}