package com.example.blooddonor.repository;

import com.example.blooddonor.entity.EmergencyRequest;
import com.example.blooddonor.entity.HospitalProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmergencyRequestRepository
        extends JpaRepository<EmergencyRequest, Long>,
        JpaSpecificationExecutor<EmergencyRequest> {

    Page<EmergencyRequest> findByHospital(
            HospitalProfile hospital,
            Pageable pageable
    );
}