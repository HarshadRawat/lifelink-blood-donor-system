package com.example.blooddonor.dto;

import com.example.blooddonor.entity.MatchStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MatchStatusRequest {

    @NotNull
    private MatchStatus status;

    private Boolean consentToShareContact;
}