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
@Table(name = "request_donor_matches")
public class RequestDonorMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "request_id")
    private EmergencyRequest emergencyRequest;

    @ManyToOne
    @JoinColumn(name = "donor_id")
    private DonorProfile donor;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    private LocalDateTime createdAt;

    @Column(name = "contact_sharing_consent")
    private Boolean contactSharingConsent = false;
}