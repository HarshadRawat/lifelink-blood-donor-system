package com.example.blooddonor.service;

import com.example.blooddonor.dto.AdminDashboardMetricsResponse;
import com.example.blooddonor.dto.AdminEmergencyRequestSummary;
import com.example.blooddonor.dto.AdminUserSummary;
import com.example.blooddonor.dto.HospitalProfileResponse;
import com.example.blooddonor.entity.EmergencyRequest;
import com.example.blooddonor.entity.HospitalProfile;
import com.example.blooddonor.entity.MatchStatus;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.entity.Role;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.repository.DonorProfileRepository;
import com.example.blooddonor.repository.EmergencyRequestRepository;
import com.example.blooddonor.repository.HospitalProfileRepository;
import com.example.blooddonor.repository.RequestDonorMatchRepository;
import com.example.blooddonor.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final HospitalProfileRepository hospitalProfileRepository;
    private final EmergencyRequestRepository emergencyRequestRepository;
    private final RequestDonorMatchRepository requestDonorMatchRepository;

    public AdminService(
            UserRepository userRepository,
            DonorProfileRepository donorProfileRepository,
            HospitalProfileRepository hospitalProfileRepository,
            EmergencyRequestRepository emergencyRequestRepository,
            RequestDonorMatchRepository requestDonorMatchRepository
    ) {
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.hospitalProfileRepository = hospitalProfileRepository;
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.requestDonorMatchRepository = requestDonorMatchRepository;
    }

    public AdminDashboardMetricsResponse getMetrics() {
        long totalUsers = userRepository.count();
        long totalDonors = userRepository.countByRole(Role.DONOR);
        long totalHospitals = userRepository.countByRole(Role.HOSPITAL);
        long totalRequests = emergencyRequestRepository.count();
        long activeRequests = emergencyRequestRepository.countByStatus(RequestStatus.ACTIVE);
        long fulfilledRequests = emergencyRequestRepository.countByStatus(RequestStatus.FULFILLED);
        long completedDonations = requestDonorMatchRepository.countByStatus(MatchStatus.COMPLETED);

        return AdminDashboardMetricsResponse.builder()
                .totalUsers(totalUsers)
                .totalDonors(totalDonors)
                .totalHospitals(totalHospitals)
                .totalEmergencyRequests(totalRequests)
                .activeEmergencyRequests(activeRequests)
                .fulfilledEmergencyRequests(fulfilledRequests)
                .completedDonations(completedDonations)
                .build();
    }

    public Page<AdminEmergencyRequestSummary> getRecentEmergencyRequests(Pageable pageable) {
        Page<EmergencyRequest> requests = emergencyRequestRepository.findAll(pageable);

        return requests.map(req -> {
            String hospitalName = req.getHospital() != null ? req.getHospital().getHospitalName() : "Unknown Hospital";
            String hospitalCity = req.getHospital() != null ? req.getHospital().getCity() : null;
            Long hospitalId = req.getHospital() != null ? req.getHospital().getId() : null;

            return AdminEmergencyRequestSummary.builder()
                    .id(req.getId())
                    .bloodGroup(req.getBloodGroup())
                    .unitsRequired(req.getUnitsRequired())
                    .urgency(req.getUrgency())
                    .status(req.getStatus())
                    .createdAt(req.getCreatedAt())
                    .hospitalId(hospitalId)
                    .hospitalName(hospitalName)
                    .hospitalCity(hospitalCity)
                    .build();
        });
    }

    public Page<AdminUserSummary> getUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);

        return users.map(user -> AdminUserSummary.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build()
        );
    }

    public Page<HospitalProfileResponse> getPendingHospitals(Pageable pageable) {
        Page<HospitalProfile> pending = hospitalProfileRepository.findByVerified(false, pageable);
        return pending.map(this::mapToHospitalProfileResponse);
    }

    @Transactional
    @CacheEvict(value = "hospital_profiles", allEntries = true)
    public HospitalProfileResponse verifyHospital(Long hospitalId, boolean verified) {
        HospitalProfile profile = hospitalProfileRepository.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital profile not found with ID: " + hospitalId));
        profile.setVerified(verified);
        HospitalProfile saved = hospitalProfileRepository.save(profile);
        return mapToHospitalProfileResponse(saved);
    }

    private HospitalProfileResponse mapToHospitalProfileResponse(HospitalProfile profile) {
        HospitalProfileResponse response = new HospitalProfileResponse();
        response.setId(profile.getId());
        if (profile.getUser() != null) {
            response.setUserId(profile.getUser().getId());
        }
        response.setHospitalName(profile.getHospitalName());
        response.setRegistrationNumber(profile.getRegistrationNumber());
        response.setAddress(profile.getAddress());
        response.setCity(profile.getCity());
        response.setLatitude(profile.getLatitude());
        response.setLongitude(profile.getLongitude());
        response.setContactNumber(profile.getContactNumber());
        response.setVerified(profile.getVerified());
        return response;
    }
}
