package com.example.blooddonor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HospitalVerificationRequest {

    @NotNull(message = "Verification status must be provided")
    private Boolean verified;
}
