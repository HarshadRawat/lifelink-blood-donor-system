package com.example.blooddonor.controller;

import com.example.blooddonor.dto.AvailabilityRequest;
import com.example.blooddonor.dto.DonorProfileRequest;
import com.example.blooddonor.dto.DonorProfileResponse;
import com.example.blooddonor.dto.RequestDonorMatchResponse;
import com.example.blooddonor.exception.BusinessRuleException;
import com.example.blooddonor.service.DonorMatchingService;
import com.example.blooddonor.service.DonorProfileService;
import com.example.blooddonor.service.JwtService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(DonorProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class DonorProfileControllerTest {


    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private JsonMapper objectMapper;


    @MockitoBean
    private DonorProfileService donorProfileService;


    @MockitoBean
    private DonorMatchingService donorMatchingService;


    @MockitoBean
    private JwtService jwtService;


    @MockitoBean
    private Authentication authentication;



    // =========================================================
    // 1. CREATE DONOR PROFILE - SUCCESS
    // =========================================================

    @Test
    void createProfile_shouldReturn200_whenValidRequest()
            throws Exception {

        DonorProfileRequest request = new DonorProfileRequest();

        request.setBloodGroup("O+");
        request.setAddress("Whitefield");
        request.setCity("Bangalore");
        request.setLatitude(12.9698);
        request.setLongitude(77.7499);
        request.setContactNumber("9876543210");


        DonorProfileResponse response = new DonorProfileResponse();

        response.setId(1L);
        response.setBloodGroup("O+");
        response.setAddress("Whitefield");
        response.setCity("Bangalore");
        response.setLatitude(12.9698);
        response.setLongitude(77.7499);
        response.setContactNumber("9876543210");
        response.setAvailable(true);


        when(authentication.getPrincipal())
                .thenReturn(9L);

        when(donorProfileService.createProfile(
                any(DonorProfileRequest.class),
                eq(9L)
        )).thenReturn(response);


        mockMvc.perform(
                        post("/api/donors/profile")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bloodGroup").value("O+"))
                .andExpect(jsonPath("$.address").value("Whitefield"))
                .andExpect(jsonPath("$.city").value("Bangalore"))
                .andExpect(jsonPath("$.available").value(true));
    }



    // =========================================================
    // 2. CREATE DONOR PROFILE - VALIDATION FAILURE
    // =========================================================

    @Test
    void createProfile_shouldReturn400_whenInvalidRequest()
            throws Exception {

        DonorProfileRequest request = new DonorProfileRequest();

        request.setBloodGroup("");
        request.setAddress("");
        request.setCity("");
        request.setContactNumber("");


        mockMvc.perform(
                        post("/api/donors/profile")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isBadRequest());
    }



    // =========================================================
    // 3. UPDATE DONOR PROFILE - SUCCESS
    // =========================================================

    @Test
    void updateProfile_shouldReturn200_whenValidRequest()
            throws Exception {

        DonorProfileRequest request = new DonorProfileRequest();

        request.setBloodGroup("O+");
        request.setAddress("Marathahalli");
        request.setCity("Bangalore");
        request.setLatitude(12.9560);
        request.setLongitude(77.7010);
        request.setContactNumber("9876543211");


        DonorProfileResponse response = new DonorProfileResponse();

        response.setId(1L);
        response.setBloodGroup("O+");
        response.setAddress("Marathahalli");
        response.setCity("Bangalore");
        response.setLatitude(12.9560);
        response.setLongitude(77.7010);
        response.setContactNumber("9876543211");
        response.setAvailable(true);


        when(authentication.getPrincipal())
                .thenReturn(9L);

        when(donorProfileService.updateProfile(
                any(DonorProfileRequest.class),
                eq(9L)
        )).thenReturn(response);


        mockMvc.perform(
                        put("/api/donors/profile")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bloodGroup").value("O+"))
                .andExpect(jsonPath("$.city").value("Bangalore"))
                .andExpect(jsonPath("$.available").value(true));
    }



    // =========================================================
    // 4. GET DONOR PROFILE - SUCCESS
    // =========================================================

    @Test
    void getProfile_shouldReturn200_whenProfileExists()
            throws Exception {

        DonorProfileResponse response = new DonorProfileResponse();

        response.setId(1L);
        response.setBloodGroup("O+");
        response.setAddress("Whitefield");
        response.setCity("Bangalore");
        response.setLatitude(12.9698);
        response.setLongitude(77.7499);
        response.setContactNumber("9876543210");
        response.setAvailable(true);


        when(authentication.getPrincipal())
                .thenReturn(9L);

        when(donorProfileService.getProfile(9L))
                .thenReturn(response);


        mockMvc.perform(
                        get("/api/donors/profile")
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bloodGroup").value("O+"))
                .andExpect(jsonPath("$.city").value("Bangalore"))
                .andExpect(jsonPath("$.available").value(true));
    }



    // =========================================================
    // 5. UPDATE AVAILABILITY - SUCCESS
    // =========================================================

    @Test
    void updateAvailability_shouldReturn200_whenValidRequest()
            throws Exception {

        AvailabilityRequest request = new AvailabilityRequest();

        request.setAvailable(false);


        DonorProfileResponse response = new DonorProfileResponse();

        response.setId(1L);
        response.setBloodGroup("O+");
        response.setAddress("Whitefield");
        response.setCity("Bangalore");
        response.setLatitude(12.9698);
        response.setLongitude(77.7499);
        response.setContactNumber("9876543210");
        response.setAvailable(false);


        when(authentication.getPrincipal())
                .thenReturn(9L);

        when(donorProfileService.updateAvailability(
                any(AvailabilityRequest.class),
                eq(9L)
        )).thenReturn(response);


        mockMvc.perform(
                        patch("/api/donors/profile/availability")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.available").value(false));
    }



    // =========================================================
    // 6. GET DONOR MATCHES - SUCCESS
    // =========================================================

    @Test
    void getMyMatches_shouldReturn200_whenMatchesExist()
            throws Exception {

        RequestDonorMatchResponse match =
                new RequestDonorMatchResponse();

        match.setId(1L);


        when(authentication.getPrincipal())
                .thenReturn(9L);

        when(donorMatchingService.getMyMatches(9L))
                .thenReturn(List.of(match));


        mockMvc.perform(
                        get("/api/donors/matches")
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }



    // =========================================================
    // 7. GET DONOR MATCHES - EMPTY LIST
    // =========================================================

    @Test
    void getMyMatches_shouldReturn200_whenNoMatchesExist()
            throws Exception {

        when(authentication.getPrincipal())
                .thenReturn(9L);

        when(donorMatchingService.getMyMatches(9L))
                .thenReturn(List.of());


        mockMvc.perform(
                        get("/api/donors/matches")
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}