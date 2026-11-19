package com.example.blooddonor.service;

import java.util.Comparator;

import com.example.blooddonor.dto.DonorContactResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DonorMatchingService {

    private final DonorProfileRepository donorProfileRepository;
    private final EmergencyRequestRepository emergencyRequestRepository;
    private final RequestDonorMatchRepository requestDonorMatchRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Clock clock;
    private ContactUnlockAuditLogRepository contactUnlockAuditLogRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private long cooldownDays = 90;

    public DonorMatchingService(
            DonorProfileRepository donorProfileRepository,
            EmergencyRequestRepository emergencyRequestRepository,
            RequestDonorMatchRepository requestDonorMatchRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this(donorProfileRepository, emergencyRequestRepository, requestDonorMatchRepository, userRepository, notificationService, Clock.systemDefaultZone(), null);
    }

    public DonorMatchingService(
            DonorProfileRepository donorProfileRepository,
            EmergencyRequestRepository emergencyRequestRepository,
            RequestDonorMatchRepository requestDonorMatchRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            Clock clock) {
        this(donorProfileRepository, emergencyRequestRepository, requestDonorMatchRepository, userRepository, notificationService, clock, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DonorMatchingService(
            DonorProfileRepository donorProfileRepository,
            EmergencyRequestRepository emergencyRequestRepository,
            RequestDonorMatchRepository requestDonorMatchRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            Clock clock,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            ContactUnlockAuditLogRepository contactUnlockAuditLogRepository) {

        this.donorProfileRepository = donorProfileRepository;
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.requestDonorMatchRepository = requestDonorMatchRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
        this.contactUnlockAuditLogRepository = contactUnlockAuditLogRepository;
    }

    public void setContactUnlockAuditLogRepository(ContactUnlockAuditLogRepository contactUnlockAuditLogRepository) {
        this.contactUnlockAuditLogRepository = contactUnlockAuditLogRepository;
    }

    public void setCooldownDays(long cooldownDays) {
        this.cooldownDays = cooldownDays;
    }

    public List<RequestDonorMatchResponse> findMatches(Long requestId) {

        // 1. Find emergency request
        EmergencyRequest emergencyRequest =
                emergencyRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Emergency request not found"));

        // Matching radius from request (supports dynamic escalation)
        final double MATCHING_RADIUS_KM = emergencyRequest.getCurrentRadiusKm() != null
                ? emergencyRequest.getCurrentRadiusKm()
                : 20.0;

        // 2. Find compatible blood groups
        List<String> compatibleBloodGroups =
                getCompatibleDonorBloodGroups(
                        emergencyRequest.getBloodGroup()
                );

        // 3. Get hospital location
        double hospitalLatitude =
                emergencyRequest.getHospital().getLatitude();

        double hospitalLongitude =
                emergencyRequest.getHospital().getLongitude();

        // 4. Calculate bounding box for indexed geolocation pre-filtering
        // 1 degree latitude ~= 111.0 km
        // 1 degree longitude ~= 111.0 * cos(latitude) km
        double deltaLat = MATCHING_RADIUS_KM / 111.0;
        double cosLat = Math.cos(Math.toRadians(hospitalLatitude));
        double deltaLng = Math.abs(cosLat) > 1e-6
                ? MATCHING_RADIUS_KM / (111.0 * Math.abs(cosLat))
                : 180.0;

        double minLat = hospitalLatitude - deltaLat;
        double maxLat = hospitalLatitude + deltaLat;
        double minLng = hospitalLongitude - deltaLng;
        double maxLng = hospitalLongitude + deltaLng;

        // Geolocation pre-filtering via composite database index
        List<DonorProfile> candidates = donorProfileRepository.findCandidates(
                compatibleBloodGroups,
                minLat,
                maxLat,
                minLng,
                maxLng
        );

        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        // Single query to fetch already matched donor IDs (eliminates N+1 exists query)
        java.util.Set<Long> alreadyMatched = requestDonorMatchRepository
                .findByEmergencyRequest(emergencyRequest).stream()
                .map(m -> m.getDonor().getId())
                .collect(java.util.stream.Collectors.toSet());

        // 5. Filter, rank and create match records (exact Haversine refinement, compute distance once)
        record CandidateDistance(DonorProfile donor, double distance) {}

        LocalDateTime cooldownCutoff = LocalDateTime.now(clock).minusDays(cooldownDays);

        List<RequestDonorMatch> matches = candidates.stream()
                .filter(donor -> donor.getLatitude() != null && donor.getLongitude() != null)
                .map(donor -> new CandidateDistance(
                        donor,
                        calculateDistance(hospitalLatitude, hospitalLongitude, donor.getLatitude(), donor.getLongitude())
                ))
                .filter(cd -> cd.distance() <= MATCHING_RADIUS_KM)
                .filter(cd -> cd.donor().getLastDonationDate() == null || cd.donor().getLastDonationDate().isBefore(cooldownCutoff))
                .filter(cd -> !alreadyMatched.contains(cd.donor().getId()))
                .sorted(Comparator.comparingDouble(CandidateDistance::distance))
                .map(cd -> {
                    RequestDonorMatch match = new RequestDonorMatch();
                    match.setEmergencyRequest(emergencyRequest);
                    match.setDonor(cd.donor());
                    match.setStatus(MatchStatus.MATCHED);
                    match.setCreatedAt(LocalDateTime.now(clock));
                    return match;
                })
                .toList();

        // 6. Save matches with idempotency guard
        List<RequestDonorMatch> savedMatches;
        try {
            savedMatches = requestDonorMatchRepository.saveAll(matches);
            // Create notification for newly matched donors
            if (savedMatches != null) {
                savedMatches.forEach(match -> {
                    notificationService.createMatchFoundNotification(
                            match.getDonor().getUser(),
                            emergencyRequest
                    );
                });
            }
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Idempotent safeguard: duplicate matches prevented by uk_request_donor unique constraint
            savedMatches = requestDonorMatchRepository.findByEmergencyRequest(emergencyRequest);
        }

        // 7. Convert entities to DTOs
        if (savedMatches == null) {
            return List.of();
        }
        return savedMatches.stream()
                .map(match -> {

                    RequestDonorMatchResponse response =
                            new RequestDonorMatchResponse();

                    response.setId(match.getId());

                    response.setEmergencyRequestId(
                            match.getEmergencyRequest().getId()
                    );

                    response.setDonorId(
                            match.getDonor().getId()
                    );

                    response.setDonorName(
                            match.getDonor().getUser().getName()
                    );

                    response.setBloodGroup(
                            match.getDonor().getBloodGroup()
                    );

                    response.setStatus(match.getStatus());

                    response.setCreatedAt(
                            match.getCreatedAt()
                    );

                    return response;
                })
                .toList();
    }

    @Transactional
    public RequestDonorMatchResponse updateMatchStatus(
            Long matchId,
            MatchStatusRequest request,
            Long userId) {

        // 1. Find the match
        RequestDonorMatch match =
                requestDonorMatchRepository.findById(matchId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Match not found"));

        // 2. Verify that this match belongs to the logged-in donor
        Long matchDonorUserId =
                match.getDonor().getUser().getId();

        if (!matchDonorUserId.equals(userId)) {
            throw new ForbiddenOperationException(
                    "You are not authorized to update this match");
        }

        // 3. Serialize on the request row via pessimistic lock
        Long reqId = match.getEmergencyRequest() != null ? match.getEmergencyRequest().getId() : null;
        EmergencyRequest emergencyRequest = (reqId != null)
                ? emergencyRequestRepository.findByIdWithPessimisticLock(reqId).orElse(match.getEmergencyRequest())
                : match.getEmergencyRequest();

        if (emergencyRequest == null) {
            throw new ResourceNotFoundException("Request not found");
        }

        if (entityManager != null) {
            entityManager.refresh(match);
        }

        // 4. Validate request is active and status transition
        MatchStatus from = match.getStatus() != null ? match.getStatus() : MatchStatus.MATCHED;
        MatchStatus to = request.getStatus();

        if (to != MatchStatus.ACCEPTED &&
                to != MatchStatus.REJECTED) {
            throw new BusinessRuleException(
                    "Invalid match status");
        }

        if (to == MatchStatus.ACCEPTED) {
            if (emergencyRequest.getStatus() != RequestStatus.ACTIVE
                    || (emergencyRequest.getExpiresAt() != null && emergencyRequest.getExpiresAt().isBefore(LocalDateTime.now(clock)))) {
                throw new BusinessRuleException(
                        "This emergency request is no longer active");
            }

            int unitsRequired = emergencyRequest.getUnitsRequired() != null ? emergencyRequest.getUnitsRequired() : 1;
            long taken = requestDonorMatchRepository.countByEmergencyRequestAndStatusIn(
                    emergencyRequest,
                    List.of(MatchStatus.ACCEPTED, MatchStatus.COMPLETED));

            if (taken >= unitsRequired) {
                throw new BusinessRuleException(
                        "Enough donors have already accepted");
            }
        }

        boolean legal = (from == MatchStatus.MATCHED || from == MatchStatus.NOTIFIED)
                || (from == MatchStatus.ACCEPTED && to == MatchStatus.REJECTED); // donor may withdraw

        if (!legal) {
            throw new BusinessRuleException(
                    "Match is already " + from);
        }

        // 5. Update status and consent
        match.setStatus(to);
        if (to == MatchStatus.ACCEPTED) {
            boolean consent = request.getConsentToShareContact() != null ? request.getConsentToShareContact() : true;
            match.setContactSharingConsent(consent);
        } else {
            match.setContactSharingConsent(false);
        }

        // 6. Save
        RequestDonorMatch updatedMatch =
                requestDonorMatchRepository.save(match);

        if (to == MatchStatus.ACCEPTED) {
            User hospitalUser =
                    emergencyRequest.getHospital().getUser();

            User donorUser =
                    match.getDonor().getUser();

            notificationService.createMatchAcceptedNotification(
                    hospitalUser,
                    emergencyRequest,
                    donorUser
            );
        } else if (to == MatchStatus.REJECTED) {
            User hospitalUser =
                    emergencyRequest.getHospital().getUser();

            User donorUser =
                    match.getDonor().getUser();

            notificationService.createMatchRejectedNotification(
                    hospitalUser,
                    emergencyRequest,
                    donorUser
            );
        }

        // 7. Convert to response DTO
        RequestDonorMatchResponse response =
                new RequestDonorMatchResponse();

        response.setId(updatedMatch.getId());
        response.setEmergencyRequestId(
                updatedMatch.getEmergencyRequest().getId()
        );
        response.setDonorId(
                updatedMatch.getDonor().getId()
        );
        response.setDonorName(
                updatedMatch.getDonor().getUser().getName()
        );
        response.setBloodGroup(
                updatedMatch.getDonor().getBloodGroup()
        );
        response.setStatus(
                updatedMatch.getStatus()
        );
        response.setCreatedAt(
                updatedMatch.getCreatedAt()
        );
        response.setContactSharingConsent(
                updatedMatch.getContactSharingConsent()
        );

        return response;
    }

    @Transactional
    public RequestDonorMatchResponse completeMatch(
            Long matchId,
            Long userId) {

        // 1. Find the match
        RequestDonorMatch match =
                requestDonorMatchRepository.findById(matchId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Match not found"));

        // 2. Find the hospital that owns this request
        Long hospitalUserId =
                match.getEmergencyRequest()
                        .getHospital()
                        .getUser()
                        .getId();

        // 3. Verify logged-in hospital
        if (!hospitalUserId.equals(userId)) {
            throw new ForbiddenOperationException(
                    "You are not authorized to complete this match");
        }

        // 4. Match must be ACCEPTED
        if (match.getStatus() != MatchStatus.ACCEPTED) {
            throw new BusinessRuleException(
                    "Only an accepted match can be completed");
        }

        // 5. Prevent over-fulfillment: serialize completions via pessimistic row-lock on EmergencyRequest
        Long reqId = match.getEmergencyRequest() != null ? match.getEmergencyRequest().getId() : null;
        EmergencyRequest emergencyRequest = (reqId != null)
                ? emergencyRequestRepository.findByIdWithPessimisticLock(reqId).orElse(match.getEmergencyRequest())
                : match.getEmergencyRequest();

        if (emergencyRequest == null) {
            throw new ResourceNotFoundException("Request not found");
        }

        if (entityManager != null) {
            entityManager.refresh(emergencyRequest, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        }

        int fulfilled = emergencyRequest.getUnitsFulfilled() != null ? emergencyRequest.getUnitsFulfilled() : 0;
        int required = emergencyRequest.getUnitsRequired() != null ? emergencyRequest.getUnitsRequired() : 1;

        if (emergencyRequest.getStatus() == RequestStatus.FULFILLED || fulfilled >= required) {
            throw new BusinessRuleException("Emergency request is already fully fulfilled");
        }

        // 6. Mark this donor match as completed and record donation date for cooldown (Task 8)
        match.setStatus(MatchStatus.COMPLETED);
        DonorProfile donor = match.getDonor();
        if (donor != null) {
            donor.setLastDonationDate(LocalDateTime.now(clock));
            donorProfileRepository.save(donor);
        }

        RequestDonorMatch updatedMatch =
                requestDonorMatchRepository.save(match);

        // 7. Increment fulfilled units and check whether quota is met
        fulfilled++;
        emergencyRequest.setUnitsFulfilled(fulfilled);

        if (fulfilled >= required) {

            // Mark the emergency request as fulfilled
            emergencyRequest.setStatus(
                    RequestStatus.FULFILLED
            );

            emergencyRequestRepository.save(
                    emergencyRequest
            );

            User hospitalUser =
                    emergencyRequest.getHospital().getUser();

            notificationService.createRequestFulfilledNotification(
                    hospitalUser,
                    emergencyRequest
            );

            // Reject all remaining unmatched donors
            List<RequestDonorMatch> remainingMatches =
                    requestDonorMatchRepository
                            .findByEmergencyRequest(emergencyRequest);

            remainingMatches.forEach(remainingMatch -> {

                if (remainingMatch.getStatus() == MatchStatus.MATCHED) {
                    remainingMatch.setStatus(MatchStatus.REJECTED);
                }
            });

            requestDonorMatchRepository.saveAll(
                    remainingMatches
            );
        } else {
            emergencyRequestRepository.save(
                    emergencyRequest
            );
        }

        // 8. Create response
        RequestDonorMatchResponse response =
                new RequestDonorMatchResponse();

        response.setId(updatedMatch.getId());

        response.setEmergencyRequestId(
                updatedMatch.getEmergencyRequest().getId()
        );

        response.setDonorId(
                updatedMatch.getDonor().getId()
        );

        response.setDonorName(
                updatedMatch.getDonor().getUser().getName()
        );

        response.setBloodGroup(
                updatedMatch.getDonor().getBloodGroup()
        );

        response.setStatus(
                updatedMatch.getStatus()
        );

        response.setCreatedAt(
                updatedMatch.getCreatedAt()
        );

        return response;
    }

    public List<RequestDonorMatchResponse> getMyMatches(Long userId) {

        // 1. Find the donor profile belonging to the logged-in user
        DonorProfile donor =
                donorProfileRepository.findByUser(
                                userRepository.findById(userId)
                                        .orElseThrow(() ->
                                                new RuntimeException("User not found"))
                        )
                        .orElseThrow(() ->
                                new RuntimeException("Donor profile not found"));

        // 2. Find all matches for this donor
        List<RequestDonorMatch> matches =
                requestDonorMatchRepository.findByDonor(donor);

        // 3. Convert entities to response DTOs
        return matches.stream()
                .map(match -> {

                    RequestDonorMatchResponse response =
                            new RequestDonorMatchResponse();

                    response.setId(match.getId());

                    response.setEmergencyRequestId(
                            match.getEmergencyRequest().getId()
                    );

                    response.setDonorId(
                            match.getDonor().getId()
                    );

                    response.setDonorName(
                            match.getDonor().getUser().getName()
                    );

                    response.setBloodGroup(
                            match.getDonor().getBloodGroup()
                    );

                    response.setStatus(match.getStatus());

                    response.setCreatedAt(
                            match.getCreatedAt()
                    );

                    return response;
                })
                .toList();
    }

    public List<RequestDonorMatchResponse> getRequestMatches(
            Long requestId,
            Long userId) {

        EmergencyRequest emergencyRequest =
                emergencyRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Emergency request not found"));

        // Check that this hospital owns the request
        if (!emergencyRequest.getHospital()
                .getUser()
                .getId()
                .equals(userId)) {

            throw new RuntimeException(
                    "You are not allowed to view this request");
        }

        List<RequestDonorMatch> matches =
                requestDonorMatchRepository
                        .findByEmergencyRequest(emergencyRequest);

        return matches.stream()
                .map(match -> {

                    RequestDonorMatchResponse response =
                            new RequestDonorMatchResponse();

                    response.setId(match.getId());

                    response.setEmergencyRequestId(
                            match.getEmergencyRequest().getId()
                    );

                    response.setDonorId(
                            match.getDonor().getId()
                    );

                    response.setDonorName(
                            match.getDonor().getUser().getName()
                    );

                    response.setBloodGroup(
                            match.getDonor().getBloodGroup()
                    );

                    response.setStatus(
                            match.getStatus()
                    );

                    response.setCreatedAt(
                            match.getCreatedAt()
                    );

                    response.setContactSharingConsent(
                            match.getContactSharingConsent()
                    );

                    // PRIVACY RULE: Expose contact information ONLY if donor accepted with consent
                    // AND the request is not cancelled
                    if (match.getStatus() == MatchStatus.ACCEPTED &&
                            Boolean.TRUE.equals(match.getContactSharingConsent()) &&
                            emergencyRequest.getStatus() != RequestStatus.CANCELLED) {

                        response.setContactNumber(match.getDonor().getContactNumber());
                        response.setEmail(match.getDonor().getUser().getEmail());
                    } else {
                        response.setContactNumber(null);
                        response.setEmail(null);
                    }

                    return response;
                })
                .toList();
    }

    public DonorContactResponse getDonorContact(
            Long requestId,
            Long matchId,
            Long userId) {

        EmergencyRequest emergencyRequest =
                emergencyRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Emergency request not found"));

        // Verify request ownership
        if (!emergencyRequest.getHospital()
                .getUser()
                .getId()
                .equals(userId)) {

            throw new SecurityException(
                    "You are not authorized to view contact details for this request");
        }

        // Verify request is not cancelled
        if (emergencyRequest.getStatus() == RequestStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Cannot view contact details for a cancelled emergency request");
        }

        RequestDonorMatch match =
                requestDonorMatchRepository.findById(matchId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Match not found"));

        // Verify match belongs to this emergency request
        if (!match.getEmergencyRequest().getId().equals(requestId)) {
            throw new IllegalArgumentException(
                    "Match does not belong to the specified emergency request");
        }

        // Verify match status is ACCEPTED
        if (match.getStatus() != MatchStatus.ACCEPTED) {
            throw new BusinessRuleException(
                    "Contact details are only available for accepted donor matches");
        }

        // Verify donor consent
        if (!Boolean.TRUE.equals(match.getContactSharingConsent())) {
            throw new BusinessRuleException(
                    "Donor has not consented to share contact details");
        }

        // Task 9 - Donor contact unlock audit logging
        if (contactUnlockAuditLogRepository != null) {
            String ipAddress = resolveClientIp();
            ContactUnlockAuditLog auditLog = new ContactUnlockAuditLog(
                    userId,
                    match.getDonor().getId(),
                    match.getId(),
                    emergencyRequest.getId(),
                    LocalDateTime.now(clock),
                    ipAddress
            );
            contactUnlockAuditLogRepository.save(auditLog);
        }

        DonorContactResponse response = new DonorContactResponse();
        response.setMatchId(match.getId());
        response.setRequestId(emergencyRequest.getId());
        response.setDonorId(match.getDonor().getId());
        response.setDonorName(match.getDonor().getUser().getName());
        response.setBloodGroup(match.getDonor().getBloodGroup());
        response.setContactNumber(match.getDonor().getContactNumber());
        response.setEmail(match.getDonor().getUser().getEmail());
        response.setStatus(match.getStatus());
        response.setContactSharingConsent(true);

        return response;
    }

    private String resolveClientIp() {
        try {
            org.springframework.web.context.request.RequestAttributes attribs =
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attribs instanceof org.springframework.web.context.request.ServletRequestAttributes servletAttribs) {
                jakarta.servlet.http.HttpServletRequest request = servletAttribs.getRequest();
                if (request != null) {
                    String forwarded = request.getHeader("X-Forwarded-For");
                    if (forwarded != null && !forwarded.isBlank()) {
                        return forwarded.split(",")[0].trim();
                    }
                    return request.getRemoteAddr();
                }
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }

    double calculateDistance(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        final int EARTH_RADIUS_KM = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(latDistance / 2)
                        * Math.sin(latDistance / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2)
                        * Math.sin(lonDistance / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return EARTH_RADIUS_KM * c;
    }

    private List<String> getCompatibleDonorBloodGroups(
            String recipientBloodGroup) {

        return switch (recipientBloodGroup.toUpperCase()) {

            case "O-" ->
                    List.of("O-");

            case "O+" ->
                    List.of("O-", "O+");

            case "A-" ->
                    List.of("O-", "A-");

            case "A+" ->
                    List.of("O-", "O+", "A-", "A+");

            case "B-" ->
                    List.of("O-", "B-");

            case "B+" ->
                    List.of("O-", "O+", "B-", "B+");

            case "AB-" ->
                    List.of("O-", "A-", "B-", "AB-");

            case "AB+" ->
                    List.of(
                            "O-", "O+",
                            "A-", "A+",
                            "B-", "B+",
                            "AB-", "AB+"
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Invalid blood group: " + recipientBloodGroup
                    );
        };
    }
}