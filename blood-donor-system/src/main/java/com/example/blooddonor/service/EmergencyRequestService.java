package com.example.blooddonor.service;

import com.example.blooddonor.dto.EmergencyRequestRequest;
import com.example.blooddonor.dto.EmergencyRequestResponse;
import com.example.blooddonor.dto.EmergencyRequestStatusRequest;
import com.example.blooddonor.entity.*;
import com.example.blooddonor.exception.BusinessRuleException;
import com.example.blooddonor.repository.EmergencyRequestRepository;
import com.example.blooddonor.repository.HospitalProfileRepository;
import com.example.blooddonor.repository.RequestDonorMatchRepository;
import com.example.blooddonor.repository.UserRepository;
import com.example.blooddonor.repository.specification.EmergencyRequestSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmergencyRequestService {

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final HospitalProfileRepository hospitalProfileRepository;
    private final UserRepository userRepository;

    private final RequestDonorMatchRepository requestDonorMatchRepository;
    private final NotificationService notificationService;

    public EmergencyRequestService(
            UserRepository userRepository,
            HospitalProfileRepository hospitalProfileRepository,
            EmergencyRequestRepository emergencyRequestRepository,
            RequestDonorMatchRepository requestDonorMatchRepository,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.hospitalProfileRepository = hospitalProfileRepository;
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.requestDonorMatchRepository = requestDonorMatchRepository;
        this.notificationService = notificationService;
    }

    public EmergencyRequestResponse createRequest(
            EmergencyRequestRequest request,
            Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        HospitalProfile hospital =
                hospitalProfileRepository.findByUser(user)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Hospital profile not found"));

        EmergencyRequest emergencyRequest =
                new EmergencyRequest();

        emergencyRequest.setBloodGroup(
                request.getBloodGroup()
        );

        emergencyRequest.setUnitsRequired(
                request.getUnitsRequired()
        );

        emergencyRequest.setUrgency(
                request.getUrgency()
        );

        emergencyRequest.setStatus(
                RequestStatus.ACTIVE
        );

        emergencyRequest.setCreatedAt(
                LocalDateTime.now()
        );

        emergencyRequest.setHospital(hospital);

        EmergencyRequest savedRequest =
                emergencyRequestRepository.save(
                        emergencyRequest
                );

        EmergencyRequestResponse response =
                new EmergencyRequestResponse();

        response.setId(savedRequest.getId());
        response.setBloodGroup(
                savedRequest.getBloodGroup()
        );
        response.setUnitsRequired(
                savedRequest.getUnitsRequired()
        );
        response.setUrgency(
                savedRequest.getUrgency()
        );
        response.setStatus(
                savedRequest.getStatus()
        );
        response.setCreatedAt(
                savedRequest.getCreatedAt()
        );
        response.setHospitalId(
                savedRequest.getHospital().getId()
        );
        response.setHospitalName(
                savedRequest.getHospital().getHospitalName()
        );

        return response;
    }

    public Page<EmergencyRequestResponse> getMyRequests(
            Long userId,
            RequestStatus status,
            String bloodGroup,
            Urgency urgency,
            Pageable pageable) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        HospitalProfile hospital =
                hospitalProfileRepository.findByUser(user)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Hospital profile not found"));

        Specification<EmergencyRequest> specification =
                EmergencyRequestSpecification.hasHospital(hospital);

        if (status != null) {
            specification = specification.and(
                    EmergencyRequestSpecification.hasStatus(status)
            );
        }

        if (bloodGroup != null) {
            specification = specification.and(
                    EmergencyRequestSpecification.hasBloodGroup(bloodGroup)
            );
        }

        if (urgency != null) {
            specification = specification.and(
                    EmergencyRequestSpecification.hasUrgency(urgency)
            );
        }

        Page<EmergencyRequest> requests =
                emergencyRequestRepository.findAll(
                        specification,
                        pageable
                );

        return requests.map(request -> {

            EmergencyRequestResponse response =
                    new EmergencyRequestResponse();

            response.setId(request.getId());
            response.setBloodGroup(request.getBloodGroup());
            response.setUnitsRequired(request.getUnitsRequired());
            response.setUrgency(request.getUrgency());
            response.setStatus(request.getStatus());
            response.setCreatedAt(request.getCreatedAt());
            response.setHospitalId(request.getHospital().getId());
            response.setHospitalName(request.getHospital().getHospitalName());

            return response;
        });
    }

    public EmergencyRequestResponse updateStatus(
            Long requestId,
            EmergencyRequestStatusRequest request,
            Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        HospitalProfile hospital =
                hospitalProfileRepository.findByUser(user)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Hospital profile not found"));

        EmergencyRequest emergencyRequest =
                emergencyRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Emergency request not found"));

        // Check ownership
        if (!emergencyRequest.getHospital().getId()
                .equals(hospital.getId())) {

            throw new BusinessRuleException(
                    "You are not allowed to update this request"
            );
        }

        // Request must currently be ACTIVE
        if (emergencyRequest.getStatus() != RequestStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Only an active request can be cancelled"
            );
        }

        // Hospital can only cancel the request manually
        if (request.getStatus() != RequestStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Hospital can only change an active request to CANCELLED"
            );
        }

        emergencyRequest.setStatus(RequestStatus.CANCELLED);

        EmergencyRequest updatedRequest =
                emergencyRequestRepository.save(
                        emergencyRequest
                );

        List<RequestDonorMatch> relevantMatches =
                requestDonorMatchRepository
                        .findByEmergencyRequestAndStatusIn(
                                emergencyRequest,
                                List.of(
                                        MatchStatus.MATCHED,
                                        MatchStatus.ACCEPTED
                                )
                        );

        for (RequestDonorMatch match : relevantMatches) {

            User donorUser =
                    match.getDonor().getUser();

            notificationService.createRequestCancelledNotification(
                    donorUser,
                    emergencyRequest
            );
        }

        EmergencyRequestResponse response =
                new EmergencyRequestResponse();

        response.setId(updatedRequest.getId());
        response.setBloodGroup(
                updatedRequest.getBloodGroup()
        );
        response.setUnitsRequired(
                updatedRequest.getUnitsRequired()
        );
        response.setUrgency(
                updatedRequest.getUrgency()
        );
        response.setStatus(
                updatedRequest.getStatus()
        );
        response.setCreatedAt(
                updatedRequest.getCreatedAt()
        );
        response.setHospitalId(
                updatedRequest.getHospital().getId()
        );
        response.setHospitalName(
                updatedRequest.getHospital().getHospitalName()
        );

        return response;
    }
}