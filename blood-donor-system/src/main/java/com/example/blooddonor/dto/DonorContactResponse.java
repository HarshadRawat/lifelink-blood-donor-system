package com.example.blooddonor.dto;

import com.example.blooddonor.entity.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonorContactResponse {

    private Long matchId;

    private Long requestId;

    private Long donorId;

    private String donorName;

    private String bloodGroup;

    private String contactNumber;

    private String email;

    private MatchStatus status;

    private Boolean contactSharingConsent;
}
