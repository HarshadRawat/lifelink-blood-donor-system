package com.example.blooddonor.service;

import com.example.blooddonor.entity.EmergencyRequest;
import com.example.blooddonor.entity.MatchStatus;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.repository.EmergencyRequestRepository;
import com.example.blooddonor.repository.RequestDonorMatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled background service for dynamic radius escalation and request expiry (Task 8).
 * - Finds ACTIVE requests with 0 accepted donors after N minutes and expands radius (e.g. 20km -> 40km).
 * - Finds expired ACTIVE requests and transitions them to CANCELLED.
 * - Idempotent execution designed for multi-instance deployments (safe to run on small fixed delay).
 */
@Service
public class EmergencyEscalationService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyEscalationService.class);

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final RequestDonorMatchRepository requestDonorMatchRepository;
    private final DonorMatchingService donorMatchingService;
    private final Clock clock;

    @Value("${app.emergency.escalation-minutes:15}")
    private long escalationMinutes = 15;

    @Value("${app.emergency.radius-step-km:20.0}")
    private double radiusStepKm = 20.0;

    @Value("${app.emergency.max-radius-km:50.0}")
    private double maxRadiusKm = 50.0;

    @Value("${app.emergency.expiry-hours:24}")
    private long expiryHours = 24;

    public EmergencyEscalationService(
            EmergencyRequestRepository emergencyRequestRepository,
            RequestDonorMatchRepository requestDonorMatchRepository,
            DonorMatchingService donorMatchingService,
            Clock clock) {
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.requestDonorMatchRepository = requestDonorMatchRepository;
        this.donorMatchingService = donorMatchingService;
        this.clock = clock;
    }

    /**
     * Periodic radius escalation job.
     * Evaluates active emergency requests lacking accepted matches and expands their radius.
     */
    @Scheduled(fixedDelayString = "${app.emergency.escalation-interval-ms:60000}")
    @Transactional
    public void escalateUnacceptedRequests() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime escalationCutoff = now.minusMinutes(escalationMinutes);

        List<EmergencyRequest> candidates = emergencyRequestRepository
                .findByStatusAndCreatedAtBefore(RequestStatus.ACTIVE, escalationCutoff);

        for (EmergencyRequest request : candidates) {
            double currentRadius = request.getCurrentRadiusKm() != null ? request.getCurrentRadiusKm() : 20.0;
            if (currentRadius >= maxRadiusKm) {
                continue;
            }

            long acceptedCount = requestDonorMatchRepository
                    .countByEmergencyRequestAndStatus(request, MatchStatus.ACCEPTED);

            if (acceptedCount == 0) {
                double expandedRadius = Math.min(maxRadiusKm, currentRadius + radiusStepKm);
                log.info("Escalating emergency request id={} radius from {} km to {} km (no accepted donors after {} min)",
                        request.getId(), currentRadius, expandedRadius, escalationMinutes);

                request.setCurrentRadiusKm(expandedRadius);
                emergencyRequestRepository.save(request);

                // Re-execute matching with expanded radius; only newly discovered donors are matched & notified
                donorMatchingService.findMatches(request.getId());
            }
        }
    }

    /**
     * Periodic request expiration job.
     * Automatically expires requests exceeding their TTL.
     */
    @Scheduled(fixedDelayString = "${app.emergency.expiry-interval-ms:60000}")
    @Transactional
    public void expireStaleRequests() {
        LocalDateTime now = LocalDateTime.now(clock);

        List<EmergencyRequest> expiredByTimestamp = emergencyRequestRepository
                .findByStatusAndExpiresAtBefore(RequestStatus.ACTIVE, now);

        for (EmergencyRequest request : expiredByTimestamp) {
            log.info("Expiring stale emergency request id={} (expired at {})", request.getId(), request.getExpiresAt());
            request.setStatus(RequestStatus.CANCELLED);
            emergencyRequestRepository.save(request);
        }
    }
}
