package com.example.blooddonor.controller;

import com.example.blooddonor.dto.HospitalProfileRequest;
import com.example.blooddonor.dto.HospitalProfileResponse;
import com.example.blooddonor.exception.BusinessRuleException;
import com.example.blooddonor.service.HospitalProfileService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(HospitalProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class HospitalProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private HospitalProfileService hospitalProfileService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private Authentication authentication;


    // =========================================================
    // 1. CREATE HOSPITAL PROFILE - SUCCESS
    // =========================================================

    @Test
    void createProfile_shouldReturn200_whenValidRequest()
            throws Exception {

        HospitalProfileRequest request =
                new HospitalProfileRequest();

        request.setHospitalName("Test Hospital");
        request.setRegistrationNumber("HOSP001");
        request.setAddress("Whitefield");
        request.setCity("Bangalore");
        request.setLatitude(12.9698);
        request.setLongitude(77.7499);
        request.setContactNumber("9876543210");


        HospitalProfileResponse response =
                new HospitalProfileResponse();

        response.setId(1L);
        response.setHospitalName("Test Hospital");
        response.setRegistrationNumber("HOSP001");
        response.setAddress("Whitefield");
        response.setCity("Bangalore");
        response.setLatitude(12.9698);
        response.setLongitude(77.7499);
        response.setContactNumber("9876543210");


        when(authentication.getPrincipal())
                .thenReturn(8L);

        when(hospitalProfileService.createProfile(
                any(HospitalProfileRequest.class),
                eq(8L)
        )).thenReturn(response);


        mockMvc.perform(
                        post("/api/hospitals/profile")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.hospitalName")
                        .value("Test Hospital"))
                .andExpect(jsonPath("$.registrationNumber")
                        .value("HOSP001"))
                .andExpect(jsonPath("$.address")
                        .value("Whitefield"))
                .andExpect(jsonPath("$.city")
                        .value("Bangalore"));
    }


    // =========================================================
    // 2. CREATE HOSPITAL PROFILE - VALIDATION FAILURE
    // =========================================================

    @Test
    void createProfile_shouldReturn400_whenInvalidRequest()
            throws Exception {

        HospitalProfileRequest request =
                new HospitalProfileRequest();

        request.setHospitalName("");
        request.setRegistrationNumber("");
        request.setAddress("");
        request.setCity("");
        request.setContactNumber("");


        mockMvc.perform(
                        post("/api/hospitals/profile")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isBadRequest());
    }


    // =========================================================
    // 3. GET HOSPITAL PROFILE - SUCCESS
    // =========================================================

    @Test
    void getProfile_shouldReturn200_whenProfileExists()
            throws Exception {

        HospitalProfileResponse response =
                new HospitalProfileResponse();

        response.setId(1L);
        response.setHospitalName("Test Hospital");
        response.setRegistrationNumber("HOSP001");
        response.setAddress("Whitefield");
        response.setCity("Bangalore");
        response.setLatitude(12.9698);
        response.setLongitude(77.7499);
        response.setContactNumber("9876543210");


        when(authentication.getPrincipal())
                .thenReturn(8L);

        when(hospitalProfileService.getProfile(8L))
                .thenReturn(response);


        mockMvc.perform(
                        get("/api/hospitals/profile")
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.hospitalName")
                        .value("Test Hospital"))
                .andExpect(jsonPath("$.registrationNumber")
                        .value("HOSP001"))
                .andExpect(jsonPath("$.city")
                        .value("Bangalore"));
    }


    // =========================================================
    // 4. UPDATE HOSPITAL PROFILE - SUCCESS
    // =========================================================

    @Test
    void updateProfile_shouldReturn200_whenValidRequest()
            throws Exception {

        HospitalProfileRequest request =
                new HospitalProfileRequest();

        request.setHospitalName("Updated Hospital");
        request.setRegistrationNumber("HOSP002");
        request.setAddress("Marathahalli");
        request.setCity("Bangalore");
        request.setLatitude(12.9560);
        request.setLongitude(77.7010);
        request.setContactNumber("9876543211");


        HospitalProfileResponse response =
                new HospitalProfileResponse();

        response.setId(1L);
        response.setHospitalName("Updated Hospital");
        response.setRegistrationNumber("HOSP002");
        response.setAddress("Marathahalli");
        response.setCity("Bangalore");
        response.setLatitude(12.9560);
        response.setLongitude(77.7010);
        response.setContactNumber("9876543211");


        when(authentication.getPrincipal())
                .thenReturn(8L);

        when(hospitalProfileService.updateProfile(
                any(HospitalProfileRequest.class),
                eq(8L)
        )).thenReturn(response);


        mockMvc.perform(
                        put("/api/hospitals/profile")
                                .principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.hospitalName")
                        .value("Updated Hospital"))
                .andExpect(jsonPath("$.registrationNumber")
                        .value("HOSP002"))
                .andExpect(jsonPath("$.address")
                        .value("Marathahalli"));
    }
}