package com.example.blooddonor.service;

import com.example.blooddonor.dto.AdminDashboardMetricsResponse;
import com.example.blooddonor.dto.AdminEmergencyRequestSummary;
import com.example.blooddonor.dto.AdminUserSummary;
import com.example.blooddonor.entity.*;
import com.example.blooddonor.repository.DonorProfileRepository;
import com.example.blooddonor.repository.EmergencyRequestRepository;
import com.example.blooddonor.repository.HospitalProfileRepository;
import com.example.blooddonor.repository.RequestDonorMatchRepository;
import com.example.blooddonor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DonorProfileRepository donorProfileRepository;

    @Mock
    private HospitalProfileRepository hospitalProfileRepository;

    @Mock
    private EmergencyRequestRepository emergencyRequestRepository;

    @Mock
    private RequestDonorMatchRepository requestDonorMatchRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                userRepository,
                donorProfileRepository,
                hospitalProfileRepository,
                emergencyRequestRepository,
                requestDonorMatchRepository
        );
    }

    @Test
    void getMetrics_shouldAggregateRepositoryCounts() {
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countByRole(Role.DONOR)).thenReturn(30L);
        when(userRepository.countByRole(Role.HOSPITAL)).thenReturn(19L);
        when(emergencyRequestRepository.count()).thenReturn(15L);
        when(emergencyRequestRepository.countByStatus(RequestStatus.ACTIVE)).thenReturn(4L);
        when(emergencyRequestRepository.countByStatus(RequestStatus.FULFILLED)).thenReturn(10L);
        when(requestDonorMatchRepository.countByStatus(MatchStatus.COMPLETED)).thenReturn(10L);

        AdminDashboardMetricsResponse response = adminService.getMetrics();

        assertNotNull(response);
        assertEquals(50L, response.getTotalUsers());
        assertEquals(30L, response.getTotalDonors());
        assertEquals(19L, response.getTotalHospitals());
        assertEquals(15L, response.getTotalEmergencyRequests());
        assertEquals(4L, response.getActiveEmergencyRequests());
        assertEquals(10L, response.getFulfilledEmergencyRequests());
        assertEquals(10L, response.getCompletedDonations());
    }

    @Test
    void getRecentEmergencyRequests_shouldMapToSummaryPage() {
        HospitalProfile hospital = new HospitalProfile();
        hospital.setId(10L);
        hospital.setHospitalName("Metro Health");
        hospital.setCity("Metropolis");

        EmergencyRequest request = new EmergencyRequest();
        request.setId(1L);
        request.setBloodGroup("A+");
        request.setUnitsRequired(3);
        request.setUrgency(Urgency.CRITICAL);
        request.setStatus(RequestStatus.ACTIVE);
        request.setCreatedAt(LocalDateTime.now());
        request.setHospital(hospital);

        PageRequest pageable = PageRequest.of(0, 10);
        when(emergencyRequestRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(request)));

        Page<AdminEmergencyRequestSummary> page = adminService.getRecentEmergencyRequests(pageable);

        assertEquals(1, page.getTotalElements());
        AdminEmergencyRequestSummary item = page.getContent().get(0);
        assertEquals(1L, item.getId());
        assertEquals("A+", item.getBloodGroup());
        assertEquals("Metro Health", item.getHospitalName());
        assertEquals("Metropolis", item.getHospitalCity());
    }

    @Test
    void getUsers_shouldMapToSummaryPage() {
        User user = new User();
        user.setId(5L);
        user.setName("Alice");
        user.setEmail("alice@test.com");
        user.setRole(Role.DONOR);

        PageRequest pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<AdminUserSummary> page = adminService.getUsers(pageable);

        assertEquals(1, page.getTotalElements());
        AdminUserSummary item = page.getContent().get(0);
        assertEquals(5L, item.getId());
        assertEquals("Alice", item.getName());
        assertEquals("alice@test.com", item.getEmail());
        assertEquals(Role.DONOR, item.getRole());
    }
}
