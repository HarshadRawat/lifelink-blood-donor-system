package com.example.blooddonor.repository;

import com.example.blooddonor.entity.EmergencyRequest;
import com.example.blooddonor.entity.HospitalProfile;
import com.example.blooddonor.entity.RequestStatus;
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

    long countByStatus(RequestStatus status);

    java.util.List<EmergencyRequest> findByStatusAndCreatedAtBefore(
            RequestStatus status,
            java.time.LocalDateTime createdAt
    );

    java.util.List<EmergencyRequest> findByStatusAndExpiresAtBefore(
            RequestStatus status,
            java.time.LocalDateTime expiresAt
    );

    @org.springframework.data.jpa.repository.Query("SELECT r FROM EmergencyRequest r WHERE r.status = :status "
            + "AND COALESCE(r.lastEscalatedAt, r.createdAt) < :cutoff")
    java.util.List<EmergencyRequest> findDueForEscalation(
            @org.springframework.data.repository.query.Param("status") RequestStatus status,
            @org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff
    );

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM emergency_requests WHERE id = :id FOR UPDATE", nativeQuery = true)
    java.util.Optional<EmergencyRequest> findByIdWithPessimisticLock(
            @org.springframework.data.repository.query.Param("id") Long id
    );
}