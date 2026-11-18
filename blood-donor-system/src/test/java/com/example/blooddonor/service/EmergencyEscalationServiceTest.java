package com.example.blooddonor.service;

import com.example.blooddonor.entity.EmergencyRequest;
import com.example.blooddonor.entity.MatchStatus;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.repository.EmergencyRequestRepository;
import com.example.blooddonor.repository.RequestDonorMatchRepository;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyEscalationServiceTest {

    @Mock
    private EmergencyRequestRepository emergencyRequestRepository;

    @Mock
    private RequestDonorMatchRepository requestDonorMatchRepository;

    @Mock
    private DonorMatchingService donorMatchingService;

    private Clock fixedClock;
    private EmergencyEscalationService escalationService;

    private final Instant baseTime = Instant.parse("2026-11-18T12:00:00Z");
    private final ZoneId zoneId = ZoneId.of("UTC");

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(baseTime, zoneId);
        escalationService = new EmergencyEscalationService(
                emergencyRequestRepository,
                requestDonorMatchRepository,
                donorMatchingService,
                fixedClock
        );
    }

    @Test
    @DisplayName("Should escalate radius from 20km to 40km when 0 donors accepted after 15 minutes")
    void shouldEscalateRadiusWhenNoAcceptedDonors() {
        LocalDateTime now = LocalDateTime.now(fixedClock);
        LocalDateTime cutoff = now.minusMinutes(15);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(101L);
        request.setStatus(RequestStatus.ACTIVE);
        request.setCurrentRadiusKm(20.0);
        request.setCreatedAt(now.minusMinutes(20));

        when(emergencyRequestRepository.findByStatusAndCreatedAtBefore(RequestStatus.ACTIVE, cutoff))
                .thenReturn(List.of(request));
        when(requestDonorMatchRepository.countByEmergencyRequestAndStatus(request, MatchStatus.ACCEPTED))
                .thenReturn(0L);

        escalationService.escalateUnacceptedRequests();

        assertEquals(40.0, request.getCurrentRadiusKm());
        verify(emergencyRequestRepository).save(request);
        verify(donorMatchingService).findMatches(101L);
    }

    @Test
    @DisplayName("Should NOT escalate radius if request has at least one accepted donor")
    void shouldNotEscalateWhenDonorAccepted() {
        LocalDateTime now = LocalDateTime.now(fixedClock);
        LocalDateTime cutoff = now.minusMinutes(15);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(102L);
        request.setStatus(RequestStatus.ACTIVE);
        request.setCurrentRadiusKm(20.0);
        request.setCreatedAt(now.minusMinutes(20));

        when(emergencyRequestRepository.findByStatusAndCreatedAtBefore(RequestStatus.ACTIVE, cutoff))
                .thenReturn(List.of(request));
        when(requestDonorMatchRepository.countByEmergencyRequestAndStatus(request, MatchStatus.ACCEPTED))
                .thenReturn(1L);

        escalationService.escalateUnacceptedRequests();

        assertEquals(20.0, request.getCurrentRadiusKm());
        verify(emergencyRequestRepository, never()).save(request);
        verify(donorMatchingService, never()).findMatches(any());
    }

    @Test
    @DisplayName("Should cap radius at max-radius (50km) and not escalate further")
    void shouldNotEscalateBeyondMaxRadius() {
        LocalDateTime now = LocalDateTime.now(fixedClock);
        LocalDateTime cutoff = now.minusMinutes(15);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(103L);
        request.setStatus(RequestStatus.ACTIVE);
        request.setCurrentRadiusKm(50.0);
        request.setCreatedAt(now.minusMinutes(35));

        when(emergencyRequestRepository.findByStatusAndCreatedAtBefore(RequestStatus.ACTIVE, cutoff))
                .thenReturn(List.of(request));

        escalationService.escalateUnacceptedRequests();

        assertEquals(50.0, request.getCurrentRadiusKm());
        verify(emergencyRequestRepository, never()).save(request);
        verify(donorMatchingService, never()).findMatches(any());
    }

    @Test
    @DisplayName("Should auto-expire stale requests past their TTL/expiresAt")
    void shouldExpireStaleRequests() {
        LocalDateTime now = LocalDateTime.now(fixedClock);

        EmergencyRequest staleRequest = new EmergencyRequest();
        staleRequest.setId(201L);
        staleRequest.setStatus(RequestStatus.ACTIVE);
        staleRequest.setExpiresAt(now.minusHours(1));

        when(emergencyRequestRepository.findByStatusAndExpiresAtBefore(RequestStatus.ACTIVE, now))
                .thenReturn(List.of(staleRequest));

        escalationService.expireStaleRequests();

        assertEquals(RequestStatus.CANCELLED, staleRequest.getStatus());
        verify(emergencyRequestRepository).save(staleRequest);
    }
}
