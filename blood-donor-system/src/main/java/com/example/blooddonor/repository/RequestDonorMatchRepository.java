package com.example.blooddonor.repository;

import com.example.blooddonor.entity.DonorProfile;
import com.example.blooddonor.entity.EmergencyRequest;
import com.example.blooddonor.entity.MatchStatus;
import com.example.blooddonor.entity.RequestDonorMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestDonorMatchRepository
        extends JpaRepository<RequestDonorMatch, Long> {

    boolean existsByEmergencyRequestAndDonor(
            EmergencyRequest emergencyRequest,
            DonorProfile donor
    );

    long countByEmergencyRequestAndStatus(
            EmergencyRequest emergencyRequest,
            MatchStatus status
    );

    List<RequestDonorMatch> findByDonor(DonorProfile donor);
    List<RequestDonorMatch> findByEmergencyRequest(
            EmergencyRequest emergencyRequest
    );

    List<RequestDonorMatch> findByEmergencyRequestAndStatusIn(
            EmergencyRequest emergencyRequest,
            List<MatchStatus> statuses
    );

}