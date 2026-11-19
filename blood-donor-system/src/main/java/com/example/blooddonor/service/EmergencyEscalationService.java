package com.example.blooddonor.service;

import com.example.blooddonor.entity.EmergencyRequest;
import com.example.blooddonor.entity.MatchStatus;
import com.example.blooddonor.entity.RequestDonorMatch;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.repository.EmergencyRequestRepository;
import com.example.blooddonor.repository.RequestDonorMatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled background service for dynamic radius escalation and request expiry.
 * - Runs single-instance in production (see ADR 018).
 * - Tracks last_escalated_at so requests escalate strictly once per 15-minute interval.
 * - Expires stale requests to EXPIRED status and notifies matched donors to prevent wasted travel.
 * - Uses per-request transactions so an isolated failure does not roll back processing for other requests.
 */
@Service
public class EmergencyEscalationService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyEscalationService.class);

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final RequestDonorMatchRepository requestDonorMatchRepository;
    private final DonorMatchingService donorMatchingService;
    private final NotificationService notificationService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.emergency.escalation-minutes:15}")
    private long escalationMinutes = 15;

    @Value("${app.emergency.radius-step-km:20.0}")
    private double radiusStepKm = 20.0;

    @Value("${app.emergency.max-radius-km:50.0}")
    private double maxRadiusKm = 50.0;

    @Value("${app.emergency.expiry-hours:24}")
    private long expiryHours = 24;

    @Autowired
    public EmergencyEscalationService(
            EmergencyRequestRepository emergencyRequestRepository,
            RequestDonorMatchRepository requestDonorMatchRepository,
            DonorMatchingService donorMatchingService,
            NotificationService notificationService,
            Clock clock,
            @Autowired(required = false) PlatformTransactionManager transactionManager) {
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.requestDonorMatchRepository = requestDonorMatchRepository;
        this.donorMatchingService = donorMatchingService;
        this.notificationService = notificationService;
        this.clock = clock;
        this.transactionTemplate = transactionManager != null ? new TransactionTemplate(transactionManager) : null;
    }

    public EmergencyEscalationService(
            EmergencyRequestRepository emergencyRequestRepository,
            RequestDonorMatchRepository requestDonorMatchRepository,
            DonorMatchingService donorMatchingService,
            Clock clock) {
        this(emergencyRequestRepository, requestDonorMatchRepository, donorMatchingService, null, clock, null);
    }

    private <T> T executeInTransaction(TransactionCallback<T> action) {
        if (transactionTemplate != null) {
            return transactionTemplate.execute(action);
        }
        return action.doInTransaction(null);
    }

    /**
     * Periodic radius escalation job.
     * Evaluates active emergency requests due for escalation (at least 15 min since creation or last escalation).
     */
    @Scheduled(fixedDelayString = "${app.emergency.escalation-interval-ms:60000}")
    public void escalateUnacceptedRequests() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime escalationCutoff = now.minusMinutes(escalationMinutes);

        List<EmergencyRequest> candidates = emergencyRequestRepository
                .findDueForEscalation(RequestStatus.ACTIVE, escalationCutoff);

        for (EmergencyRequest candidate : candidates) {
            try {
                executeInTransaction(status -> {
                    escalateSingleRequest(candidate.getId(), now);
                    return null;
                });
            } catch (Exception ex) {
                log.error("Failed to escalate emergency request id={}: {}", candidate.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void escalateSingleRequest(Long requestId, LocalDateTime now) {
        EmergencyRequest request = emergencyRequestRepository.findByIdWithPessimisticLock(requestId)
                .orElse(null);
        if (request == null || request.getStatus() != RequestStatus.ACTIVE) {
            return;
        }

        double currentRadius = request.getCurrentRadiusKm() != null ? request.getCurrentRadiusKm() : 20.0;
        if (currentRadius >= maxRadiusKm) {
            return;
        }

        long acceptedCount = requestDonorMatchRepository
                .countByEmergencyRequestAndStatus(request, MatchStatus.ACCEPTED);

        if (acceptedCount == 0) {
            double expandedRadius = Math.min(maxRadiusKm, currentRadius + radiusStepKm);
            log.info("Escalating emergency request id={} radius from {} km to {} km (no accepted donors after {} min)",
                    request.getId(), currentRadius, expandedRadius, escalationMinutes);

            request.setCurrentRadiusKm(expandedRadius);
            request.setLastEscalatedAt(now);
            emergencyRequestRepository.save(request);

            // Re-execute matching with expanded radius; only newly discovered donors are matched & notified
            donorMatchingService.findMatches(request.getId());
        }
    }

    /**
     * Periodic request expiration job.
     * Automatically transitions requests exceeding their TTL to EXPIRED and alerts matched donors.
     */
    @Scheduled(fixedDelayString = "${app.emergency.expiry-interval-ms:60000}")
    public void expireStaleRequests() {
        LocalDateTime now = LocalDateTime.now(clock);

        List<EmergencyRequest> expiredByTimestamp = emergencyRequestRepository
                .findByStatusAndExpiresAtBefore(RequestStatus.ACTIVE, now);

        for (EmergencyRequest candidate : expiredByTimestamp) {
            try {
                executeInTransaction(status -> {
                    expireSingleRequest(candidate.getId());
                    return null;
                });
            } catch (Exception ex) {
                log.error("Failed to expire emergency request id={}: {}", candidate.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void expireSingleRequest(Long requestId) {
        EmergencyRequest request = emergencyRequestRepository.findByIdWithPessimisticLock(requestId)
                .orElse(null);
        if (request == null || request.getStatus() != RequestStatus.ACTIVE) {
            return;
        }

        log.info("Expiring stale emergency request id={} (expired at {})", request.getId(), request.getExpiresAt());
        request.setStatus(RequestStatus.EXPIRED);
        emergencyRequestRepository.save(request);

        List<RequestDonorMatch> matches = requestDonorMatchRepository
                .findByEmergencyRequestAndStatusIn(
                        request,
                        List.of(MatchStatus.MATCHED, MatchStatus.NOTIFIED, MatchStatus.ACCEPTED)
                );

        if (notificationService != null) {
            for (RequestDonorMatch match : matches) {
                if (match.getDonor() != null && match.getDonor().getUser() != null) {
                    notificationService.createRequestCancelledNotification(
                            match.getDonor().getUser(),
                            request
                    );
                }
            }
        }
    }
}
