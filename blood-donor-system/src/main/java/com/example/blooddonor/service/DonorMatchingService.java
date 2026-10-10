package com.example.blooddonor.service;

import java.util.Comparator;

import com.example.blooddonor.dto.DonorContactResponse;
import com.example.blooddonor.dto.MatchStatusRequest;
import com.example.blooddonor.dto.RequestDonorMatchResponse;
import com.example.blooddonor.entity.*;
import com.example.blooddonor.exception.BusinessRuleException;
import com.example.blooddonor.repository.DonorProfileRepository;
import com.example.blooddonor.repository.EmergencyRequestRepository;
import com.example.blooddonor.repository.RequestDonorMatchRepository;
import com.example.blooddonor.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DonorMatchingService {

    private final DonorProfileRepository donorProfileRepository;
    private final EmergencyRequestRepository emergencyRequestRepository;
    private final RequestDonorMatchRepository requestDonorMatchRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;


    public DonorMatchingService(
            DonorProfileRepository donorProfileRepository,
            EmergencyRequestRepository emergencyRequestRepository,
            RequestDonorMatchRepository requestDonorMatchRepository,
            UserRepository userRepository,
            NotificationService notificationService) {

        this.donorProfileRepository = donorProfileRepository;
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.requestDonorMatchRepository = requestDonorMatchRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public List<RequestDonorMatchResponse> findMatches(Long requestId) {

        // Matching radius
        final double MATCHING_RADIUS_KM = 20.0;
        // 1. Find emergency request
        EmergencyRequest emergencyRequest =
                emergencyRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Emergency request not found"));

        // 2. Find compatible and available donors
        List<String> compatibleBloodGroups =
                getCompatibleDonorBloodGroups(
                        emergencyRequest.getBloodGroup()
                );

        List<DonorProfile> donors =
                donorProfileRepository.findByBloodGroupInAndAvailable(
                        compatibleBloodGroups,
                        true
                );

        // 3. Get hospital location
        double hospitalLatitude =
                emergencyRequest.getHospital().getLatitude();

        double hospitalLongitude =
                emergencyRequest.getHospital().getLongitude();

        // 4. Filter, rank and create match records
        List<RequestDonorMatch> matches = donors.stream()

                // Donor must be within 10 km
                .filter(donor -> {

                    double distance = calculateDistance(
                            hospitalLatitude,
                            hospitalLongitude,
                            donor.getLatitude(),
                            donor.getLongitude()
                    );

                    return distance <= MATCHING_RADIUS_KM;
                })

                // Avoid duplicate matches
                .filter(donor ->
                        !requestDonorMatchRepository
                                .existsByEmergencyRequestAndDonor(
                                        emergencyRequest,
                                        donor
                                )
                )

                // Rank nearest donor first
                .sorted(Comparator.comparingDouble(donor ->
                        calculateDistance(
                                hospitalLatitude,
                                hospitalLongitude,
                                donor.getLatitude(),
                                donor.getLongitude()
                        )
                ))

                .map(donor -> {

                    RequestDonorMatch match =
                            new RequestDonorMatch();

                    match.setEmergencyRequest(emergencyRequest);
                    match.setDonor(donor);
                    match.setStatus(MatchStatus.MATCHED);
                    match.setCreatedAt(LocalDateTime.now());

                    return match;
                })
                .toList();

        // 5. Save matches
        List<RequestDonorMatch> savedMatches =
                requestDonorMatchRepository.saveAll(matches);

        // 6. Create notification for every matched donor
        savedMatches.forEach(match -> {

            notificationService.createMatchFoundNotification(
                    match.getDonor().getUser(),
                    emergencyRequest
            );
        });

        // 7. Convert entities to DTOs
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

    public RequestDonorMatchResponse updateMatchStatus(
            Long matchId,
            MatchStatusRequest request,
            Long userId) {

        // 1. Find the match
        RequestDonorMatch match =
                requestDonorMatchRepository.findById(matchId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Match not found"));

        // 2. Get the emergency request
        EmergencyRequest emergencyRequest =
                match.getEmergencyRequest();

        // 3. Prevent donor from accepting an inactive request
        if (request.getStatus() == MatchStatus.ACCEPTED &&
                emergencyRequest.getStatus() != RequestStatus.ACTIVE) {

            throw new BusinessRuleException(
                    "This emergency request is no longer active");
        }

        // 4. Verify that this match belongs to the logged-in donor
        Long matchDonorUserId =
                match.getDonor().getUser().getId();

        if (!matchDonorUserId.equals(userId)) {
            throw new RuntimeException(
                    "You are not authorized to update this match");
        }

        // 5. Allow only ACCEPTED or REJECTED
        MatchStatus status = request.getStatus();

        if (status != MatchStatus.ACCEPTED &&
                status != MatchStatus.REJECTED) {

            throw new RuntimeException(
                    "Invalid match status");
        }

        // 6. Update status and consent
        match.setStatus(status);
        if (status == MatchStatus.ACCEPTED) {
            boolean consent = request.getConsentToShareContact() != null ? request.getConsentToShareContact() : true;
            match.setContactSharingConsent(consent);
        } else {
            match.setContactSharingConsent(false);
        }

        // 7. Save
        RequestDonorMatch updatedMatch =
                requestDonorMatchRepository.save(match);

        if (status == MatchStatus.ACCEPTED) {

            User hospitalUser =
                    emergencyRequest.getHospital().getUser();

            User donorUser =
                    match.getDonor().getUser();

            notificationService.createMatchAcceptedNotification(
                    hospitalUser,
                    emergencyRequest,
                    donorUser
            );
        }

        if (status == MatchStatus.REJECTED) {

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

        // 8. Convert to response DTO
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
                                new RuntimeException(
                                        "Match not found"));

        // 2. Find the hospital that owns this request
        Long hospitalUserId =
                match.getEmergencyRequest()
                        .getHospital()
                        .getUser()
                        .getId();

        // 3. Verify logged-in hospital
        if (!hospitalUserId.equals(userId)) {
            throw new RuntimeException(
                    "You are not authorized to complete this match");
        }

        // 4. Match must be ACCEPTED
        if (match.getStatus() != MatchStatus.ACCEPTED) {
            throw new RuntimeException(
                    "Only an accepted match can be completed");
        }

        // 5. Mark this donor match as completed
        match.setStatus(MatchStatus.COMPLETED);

        RequestDonorMatch updatedMatch =
                requestDonorMatchRepository.save(match);

        // 6. Check how many donations are completed
        EmergencyRequest emergencyRequest =
                updatedMatch.getEmergencyRequest();

        long completedUnits =
                requestDonorMatchRepository
                        .countByEmergencyRequestAndStatus(
                                emergencyRequest,
                                MatchStatus.COMPLETED
                        );

        // 7. Check whether required units are fulfilled
        if (completedUnits >=
                emergencyRequest.getUnitsRequired()) {

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

    private double calculateDistance(
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