package com.example.blooddonor.repository;

import com.example.blooddonor.entity.ContactUnlockAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactUnlockAuditLogRepository extends JpaRepository<ContactUnlockAuditLog, Long> {

    List<ContactUnlockAuditLog> findByHospitalUserId(Long hospitalUserId);

    List<ContactUnlockAuditLog> findByDonorId(Long donorId);

    Page<ContactUnlockAuditLog> findAllByOrderByUnlockedAtDesc(Pageable pageable);
}
