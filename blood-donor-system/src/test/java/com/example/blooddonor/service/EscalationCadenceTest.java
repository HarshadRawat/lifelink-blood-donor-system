package com.example.blooddonor.service;

import com.example.blooddonor.entity.EmergencyRequest;
import com.example.blooddonor.entity.MatchStatus;
import com.example.blooddonor.entity.RequestDonorMatch;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.entity.DonorProfile;
import com.example.blooddonor.repository.EmergencyRequestRepository;
import com.example.blooddonor.repository.RequestDonorMatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EscalationCadenceTest {

    @Mock
    private EmergencyRequestRepository emergencyRequestRepository;

    @Mock
    private RequestDonorMatchRepository requestDonorMatchRepository;

    @Mock
    private DonorMatchingService donorMatchingService;

    @Mock
    private NotificationService notificationService;

    private final Instant t0 = Instant.parse("2026-11-19T10:00:00Z");
    private final ZoneId zone = ZoneId.of("UTC");

    @Test
    @DisplayName("Radius progresses 20km -> 40km -> 50km over 30 minutes, never escalating twice within 15 minutes")
    void escalationCadence_progressesCorrectlyOverTime() {
        LocalDateTime createdTime = LocalDateTime.ofInstant(t0, zone);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(777L);
        request.setStatus(RequestStatus.ACTIVE);
        request.setCurrentRadiusKm(20.0);
        request.setCreatedAt(createdTime);
        request.setLastEscalatedAt(null);

        // 1. T = 16 minutes after creation: First escalation must succeed
        Instant tPlus16m = t0.plusSeconds(16 * 60);
        Clock clock16 = Clock.fixed(tPlus16m, zone);
        LocalDateTime now16 = LocalDateTime.ofInstant(tPlus16m, zone);
        LocalDateTime cutoff16 = now16.minusMinutes(15);

        EmergencyEscalationService service16 = new EmergencyEscalationService(
                emergencyRequestRepository,
                requestDonorMatchRepository,
                donorMatchingService,
                notificationService,
                clock16,
                null
        );

        when(emergencyRequestRepository.findDueForEscalation(RequestStatus.ACTIVE, cutoff16))
                .thenReturn(List.of(request));
        when(emergencyRequestRepository.findByIdWithPessimisticLock(777L))
                .thenReturn(Optional.of(request));
        when(requestDonorMatchRepository.countByEmergencyRequestAndStatus(request, MatchStatus.ACCEPTED))
                .thenReturn(0L);

        service16.escalateUnacceptedRequests();

        assertEquals(40.0, request.getCurrentRadiusKm());
        assertEquals(now16, request.getLastEscalatedAt());
        verify(emergencyRequestRepository).save(request);
        verify(donorMatchingService).findMatches(777L);

        // 2. T = 17 minutes (1 minute after first escalation): Request is NOT due
        Instant tPlus17m = t0.plusSeconds(17 * 60);
        Clock clock17 = Clock.fixed(tPlus17m, zone);
        LocalDateTime now17 = LocalDateTime.ofInstant(tPlus17m, zone);
        LocalDateTime cutoff17 = now17.minusMinutes(15);

        EmergencyEscalationService service17 = new EmergencyEscalationService(
                emergencyRequestRepository,
                requestDonorMatchRepository,
                donorMatchingService,
                notificationService,
                clock17,
                null
        );

        // COALESCE(lastEscalatedAt, createdAt) is now16. now16 > cutoff17, so repository returns empty list
        when(emergencyRequestRepository.findDueForEscalation(RequestStatus.ACTIVE, cutoff17))
                .thenReturn(List.of());

        service17.escalateUnacceptedRequests();

        // Must still be 40.0 km
        assertEquals(40.0, request.getCurrentRadiusKm());

        // 3. T = 32 minutes (16 minutes after first escalation): Second escalation to 50km
        Instant tPlus32m = t0.plusSeconds(32 * 60);
        Clock clock32 = Clock.fixed(tPlus32m, zone);
        LocalDateTime now32 = LocalDateTime.ofInstant(tPlus32m, zone);
        LocalDateTime cutoff32 = now32.minusMinutes(15);

        EmergencyEscalationService service32 = new EmergencyEscalationService(
                emergencyRequestRepository,
                requestDonorMatchRepository,
                donorMatchingService,
                notificationService,
                clock32,
                null
        );

        when(emergencyRequestRepository.findDueForEscalation(RequestStatus.ACTIVE, cutoff32))
                .thenReturn(List.of(request));

        service32.escalateUnacceptedRequests();

        assertEquals(50.0, request.getCurrentRadiusKm());
        assertEquals(now32, request.getLastEscalatedAt());
    }

    @Test
    @DisplayName("Request auto-expires to EXPIRED status and alerts donors when TTL passes")
    void expireStaleRequests_transitionsToEXPIREDAndAlertsDonors() {
        Instant tExpired = t0.plusSeconds(25 * 3600); // 25 hours later
        Clock clock = Clock.fixed(tExpired, zone);
        LocalDateTime now = LocalDateTime.ofInstant(tExpired, zone);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(888L);
        request.setStatus(RequestStatus.ACTIVE);
        request.setExpiresAt(now.minusMinutes(5));

        User donorUser = new User();
        donorUser.setId(901L);
        DonorProfile donorProfile = new DonorProfile();
        donorProfile.setUser(donorUser);

        RequestDonorMatch match = new RequestDonorMatch();
        match.setDonor(donorProfile);
        match.setEmergencyRequest(request);
        match.setStatus(MatchStatus.ACCEPTED);

        EmergencyEscalationService service = new EmergencyEscalationService(
                emergencyRequestRepository,
                requestDonorMatchRepository,
                donorMatchingService,
                notificationService,
                clock,
                null
        );

        when(emergencyRequestRepository.findByStatusAndExpiresAtBefore(RequestStatus.ACTIVE, now))
                .thenReturn(List.of(request));
        when(emergencyRequestRepository.findByIdWithPessimisticLock(888L))
                .thenReturn(Optional.of(request));
        when(requestDonorMatchRepository.findByEmergencyRequestAndStatusIn(eq(request), anyList()))
                .thenReturn(List.of(match));

        service.expireStaleRequests();

        assertEquals(RequestStatus.EXPIRED, request.getStatus());
        verify(emergencyRequestRepository).save(request);
        verify(notificationService).createRequestCancelledNotification(donorUser, request);
    }
}
