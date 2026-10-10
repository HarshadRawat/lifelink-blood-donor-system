package com.example.blooddonor.dto;

import com.example.blooddonor.entity.MatchStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RequestDonorMatchResponse {

    private Long id;

    private Long emergencyRequestId;

    private Long donorId;

    private String donorName;

    private String bloodGroup;

    private MatchStatus status;

    private LocalDateTime createdAt;

    private String contactNumber;

    private String email;

    private Boolean contactSharingConsent;
}