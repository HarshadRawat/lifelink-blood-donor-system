package com.example.blooddonor.dto;

import com.example.blooddonor.entity.Urgency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmergencyRequestRequest {

    @NotBlank
    private String bloodGroup;

    @NotNull
    @Min(1)
    private Integer unitsRequired;

    @NotNull
    private Urgency urgency;
}