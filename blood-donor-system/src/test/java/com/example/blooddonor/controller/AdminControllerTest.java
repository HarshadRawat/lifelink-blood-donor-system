package com.example.blooddonor.controller;

import com.example.blooddonor.dto.AdminDashboardMetricsResponse;
import com.example.blooddonor.dto.AdminEmergencyRequestSummary;
import com.example.blooddonor.dto.AdminUserSummary;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.entity.Role;
import com.example.blooddonor.entity.Urgency;
import com.example.blooddonor.service.AdminService;
import com.example.blooddonor.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getMetrics_shouldReturn200_andMetricsPayload() throws Exception {
        AdminDashboardMetricsResponse metrics = AdminDashboardMetricsResponse.builder()
                .totalUsers(25)
                .totalDonors(15)
                .totalHospitals(10)
                .totalEmergencyRequests(8)
                .activeEmergencyRequests(3)
                .fulfilledEmergencyRequests(5)
                .completedDonations(5)
                .build();

        when(adminService.getMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/admin/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(25))
                .andExpect(jsonPath("$.totalDonors").value(15))
                .andExpect(jsonPath("$.totalHospitals").value(10))
                .andExpect(jsonPath("$.totalEmergencyRequests").value(8))
                .andExpect(jsonPath("$.activeEmergencyRequests").value(3))
                .andExpect(jsonPath("$.fulfilledEmergencyRequests").value(5))
                .andExpect(jsonPath("$.completedDonations").value(5));
    }

    @Test
    void getRecentRequests_shouldReturn200_andPage() throws Exception {
        AdminEmergencyRequestSummary summary = AdminEmergencyRequestSummary.builder()
                .id(1L)
                .bloodGroup("O+")
                .unitsRequired(2)
                .urgency(Urgency.HIGH)
                .status(RequestStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .hospitalId(10L)
                .hospitalName("City General Hospital")
                .hospitalCity("Seattle")
                .build();

        when(adminService.getRecentEmergencyRequests(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary)));

        mockMvc.perform(get("/api/admin/requests?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].bloodGroup").value("O+"))
                .andExpect(jsonPath("$.content[0].hospitalName").value("City General Hospital"));
    }

    @Test
    void getUsers_shouldReturn200_andPage() throws Exception {
        AdminUserSummary userSummary = AdminUserSummary.builder()
                .id(1L)
                .name("John Donor")
                .email("john@donor.com")
                .role(Role.DONOR)
                .build();

        when(adminService.getUsers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(userSummary)));

        mockMvc.perform(get("/api/admin/users?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("John Donor"))
                .andExpect(jsonPath("$.content[0].email").value("john@donor.com"))
                .andExpect(jsonPath("$.content[0].role").value("DONOR"));
    }
}
