package com.example.blooddonor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "contact_unlock_audit_logs", indexes = {
        @Index(name = "idx_audit_hospital_user", columnList = "hospital_user_id"),
        @Index(name = "idx_audit_donor", columnList = "donor_id"),
        @Index(name = "idx_audit_match", columnList = "match_id")
})
public class ContactUnlockAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hospital_user_id", nullable = false)
    private Long hospitalUserId;

    @Column(name = "donor_id", nullable = false)
    private Long donorId;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "emergency_request_id", nullable = false)
    private Long emergencyRequestId;

    @Column(name = "unlocked_at", nullable = false)
    private LocalDateTime unlockedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    public ContactUnlockAuditLog(Long hospitalUserId, Long donorId, Long matchId, Long emergencyRequestId, LocalDateTime unlockedAt, String ipAddress) {
        this.hospitalUserId = hospitalUserId;
        this.donorId = donorId;
        this.matchId = matchId;
        this.emergencyRequestId = emergencyRequestId;
        this.unlockedAt = unlockedAt;
        this.ipAddress = ipAddress;
    }
}
