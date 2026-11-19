package com.example.blooddonor.service;

import com.example.blooddonor.dto.RequestDonorMatchResponse;
import com.example.blooddonor.entity.EmergencyRequest;
import com.example.blooddonor.entity.HospitalProfile;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.repository.ContactUnlockAuditLogRepository;
import com.example.blooddonor.repository.DonorProfileRepository;
import com.example.blooddonor.repository.EmergencyRequestRepository;
import com.example.blooddonor.repository.RequestDonorMatchRepository;
import com.example.blooddonor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Step 3.3 & Step 4.2: Verify that when no donors are nearby in the bounding box,
 * exactly one indexed query is executed and NO nationwide fallback full-table scan occurs.
 */
@ExtendWith(MockitoExtension.class)
class NoDonorsNearbyMatchingTest {

    @Mock
    private DonorProfileRepository donorProfileRepository;

    @Mock
    private EmergencyRequestRepository emergencyRequestRepository;

    @Mock
    private RequestDonorMatchRepository requestDonorMatchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ContactUnlockAuditLogRepository contactUnlockAuditLogRepository;

    private DonorMatchingService donorMatchingService;

    @BeforeEach
    void setUp() {
        donorMatchingService = new DonorMatchingService(
                donorProfileRepository,
                emergencyRequestRepository,
                requestDonorMatchRepository,
                userRepository,
                notificationService,
                Clock.systemUTC(),
                contactUnlockAuditLogRepository
        );
    }

    @Test
    @DisplayName("Request with no donors nearby executes exactly one indexed query and no fallback scans")
    void noDonorsNearby_executesOneIndexedQueryAndNoScanFallback() {
        User hospitalUser = new User();
        hospitalUser.setId(1L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setId(10L);
        hospital.setUser(hospitalUser);
        hospital.setLatitude(12.9716);
        hospital.setLongitude(77.5946);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(100L);
        request.setBloodGroup("AB-");
        request.setUnitsRequired(2);
        request.setStatus(RequestStatus.ACTIVE);
        request.setHospital(hospital);
        request.setCurrentRadiusKm(20.0);

        when(emergencyRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(donorProfileRepository.findCandidates(anyList(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of()); // No candidates in bounding box

        List<RequestDonorMatchResponse> matches = donorMatchingService.findMatches(100L);

        // Verification
        assertTrue(matches.isEmpty(), "Matches must be empty when no donors are nearby");

        // Exactly one indexed candidate query executed
        verify(donorProfileRepository, times(1)).findCandidates(
                anyList(), anyDouble(), anyDouble(), anyDouble(), anyDouble()
        );

        // No match persistence or notification calls
        verify(requestDonorMatchRepository, never()).saveAll(anyList());
        verify(notificationService, never()).createMatchFoundNotification(any(), any());
    }
}
