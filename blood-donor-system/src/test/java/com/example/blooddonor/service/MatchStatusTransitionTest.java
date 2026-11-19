package com.example.blooddonor.service;

import com.example.blooddonor.dto.MatchStatusRequest;
import com.example.blooddonor.dto.RequestDonorMatchResponse;
import com.example.blooddonor.entity.*;
import com.example.blooddonor.exception.BusinessRuleException;
import com.example.blooddonor.exception.ForbiddenOperationException;
import com.example.blooddonor.exception.ResourceNotFoundException;
import com.example.blooddonor.repository.ContactUnlockAuditLogRepository;
import com.example.blooddonor.repository.DonorProfileRepository;
import com.example.blooddonor.repository.EmergencyRequestRepository;
import com.example.blooddonor.repository.RequestDonorMatchRepository;
import com.example.blooddonor.repository.UserRepository;
import jakarta.persistence.EntityManager;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchStatusTransitionTest {

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

    @Mock
    private EntityManager entityManager;

    private Clock fixedClock;
    private DonorMatchingService donorMatchingService;

    private User donorUser;
    private DonorProfile donorProfile;
    private User hospitalUser;
    private HospitalProfile hospitalProfile;
    private EmergencyRequest emergencyRequest;
    private RequestDonorMatch match;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneId.of("UTC"));
        donorMatchingService = new DonorMatchingService(
                donorProfileRepository,
                emergencyRequestRepository,
                requestDonorMatchRepository,
                userRepository,
                notificationService,
                fixedClock,
                contactUnlockAuditLogRepository
        );

        donorUser = new User();
        donorUser.setId(100L);
        donorUser.setName("Donor John");
        donorUser.setEmail("donor@example.com");

        donorProfile = new DonorProfile();
        donorProfile.setId(200L);
        donorProfile.setUser(donorUser);
        donorProfile.setBloodGroup("O+");

        hospitalUser = new User();
        hospitalUser.setId(300L);
        hospitalUser.setName("City Hospital");

        hospitalProfile = new HospitalProfile();
        hospitalProfile.setId(400L);
        hospitalProfile.setUser(hospitalUser);

        emergencyRequest = new EmergencyRequest();
        emergencyRequest.setId(500L);
        emergencyRequest.setHospital(hospitalProfile);
        emergencyRequest.setStatus(RequestStatus.ACTIVE);
        emergencyRequest.setUnitsRequired(2);
        emergencyRequest.setUnitsFulfilled(0);
        emergencyRequest.setExpiresAt(LocalDateTime.now(fixedClock).plusHours(2));

        match = new RequestDonorMatch();
        match.setId(600L);
        match.setDonor(donorProfile);
        match.setEmergencyRequest(emergencyRequest);
        match.setStatus(MatchStatus.MATCHED);
        match.setCreatedAt(LocalDateTime.now(fixedClock).minusMinutes(5));
    }

    @Test
    @DisplayName("COMPLETED match cannot revert to REJECTED or ACCEPTED")
    void completedMatch_cannotRevert() {
        match.setStatus(MatchStatus.COMPLETED);
        when(requestDonorMatchRepository.findById(600L)).thenReturn(Optional.of(match));
        when(emergencyRequestRepository.findByIdWithPessimisticLock(500L)).thenReturn(Optional.of(emergencyRequest));

        MatchStatusRequest req = new MatchStatusRequest();
        req.setStatus(MatchStatus.REJECTED);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                donorMatchingService.updateMatchStatus(600L, req, 100L));
        assertTrue(ex.getMessage().contains("Match is already COMPLETED"));
    }

    @Test
    @DisplayName("ACCEPTED donor may withdraw to REJECTED")
    void acceptedDonor_canWithdrawToRejected() {
        match.setStatus(MatchStatus.ACCEPTED);
        when(requestDonorMatchRepository.findById(600L)).thenReturn(Optional.of(match));
        when(emergencyRequestRepository.findByIdWithPessimisticLock(500L)).thenReturn(Optional.of(emergencyRequest));
        when(requestDonorMatchRepository.save(any(RequestDonorMatch.class))).thenAnswer(i -> i.getArgument(0));

        MatchStatusRequest req = new MatchStatusRequest();
        req.setStatus(MatchStatus.REJECTED);

        RequestDonorMatchResponse res = donorMatchingService.updateMatchStatus(600L, req, 100L);
        assertEquals(MatchStatus.REJECTED, res.getStatus());
        assertFalse(res.getContactSharingConsent());
    }

    @Test
    @DisplayName("MATCHED donor can accept within active window and available quota")
    void matchedDonor_canAccept() {
        when(requestDonorMatchRepository.findById(600L)).thenReturn(Optional.of(match));
        when(emergencyRequestRepository.findByIdWithPessimisticLock(500L)).thenReturn(Optional.of(emergencyRequest));
        when(requestDonorMatchRepository.countByEmergencyRequestAndStatusIn(eq(emergencyRequest), anyCollection()))
                .thenReturn(0L);
        when(requestDonorMatchRepository.save(any(RequestDonorMatch.class))).thenAnswer(i -> i.getArgument(0));

        MatchStatusRequest req = new MatchStatusRequest();
        req.setStatus(MatchStatus.ACCEPTED);
        req.setConsentToShareContact(true);

        RequestDonorMatchResponse res = donorMatchingService.updateMatchStatus(600L, req, 100L);
        assertEquals(MatchStatus.ACCEPTED, res.getStatus());
        assertTrue(res.getContactSharingConsent());
        verify(notificationService).createMatchAcceptedNotification(eq(hospitalUser), eq(emergencyRequest), eq(donorUser));
    }

    @Test
    @DisplayName("Accept rejected when request has expired")
    void accept_rejectedWhenRequestExpired() {
        emergencyRequest.setExpiresAt(LocalDateTime.now(fixedClock).minusMinutes(1));
        when(requestDonorMatchRepository.findById(600L)).thenReturn(Optional.of(match));
        when(emergencyRequestRepository.findByIdWithPessimisticLock(500L)).thenReturn(Optional.of(emergencyRequest));

        MatchStatusRequest req = new MatchStatusRequest();
        req.setStatus(MatchStatus.ACCEPTED);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                donorMatchingService.updateMatchStatus(600L, req, 100L));
        assertEquals("This emergency request is no longer active", ex.getMessage());
    }

    @Test
    @DisplayName("Accept rejected when accept quota is already exhausted")
    void accept_rejectedWhenQuotaExhausted() {
        emergencyRequest.setUnitsRequired(2);
        when(requestDonorMatchRepository.findById(600L)).thenReturn(Optional.of(match));
        when(emergencyRequestRepository.findByIdWithPessimisticLock(500L)).thenReturn(Optional.of(emergencyRequest));
        when(requestDonorMatchRepository.countByEmergencyRequestAndStatusIn(eq(emergencyRequest), anyCollection()))
                .thenReturn(2L); // 2 units already taken

        MatchStatusRequest req = new MatchStatusRequest();
        req.setStatus(MatchStatus.ACCEPTED);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                donorMatchingService.updateMatchStatus(600L, req, 100L));
        assertEquals("Enough donors have already accepted", ex.getMessage());
    }

    @Test
    @DisplayName("Another donor updating match returns 403 ForbiddenOperationException")
    void anotherDonorUpdatingMatch_returnsForbidden() {
        when(requestDonorMatchRepository.findById(600L)).thenReturn(Optional.of(match));

        MatchStatusRequest req = new MatchStatusRequest();
        req.setStatus(MatchStatus.ACCEPTED);

        assertThrows(ForbiddenOperationException.class, () ->
                donorMatchingService.updateMatchStatus(600L, req, 999L)); // 999 != 100
    }

    @Test
    @DisplayName("Match not found returns 404 ResourceNotFoundException")
    void matchNotFound_returnsNotFound() {
        when(requestDonorMatchRepository.findById(999L)).thenReturn(Optional.empty());

        MatchStatusRequest req = new MatchStatusRequest();
        req.setStatus(MatchStatus.ACCEPTED);

        assertThrows(ResourceNotFoundException.class, () ->
                donorMatchingService.updateMatchStatus(999L, req, 100L));
    }
}
