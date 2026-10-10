package com.example.blooddonor.dto;

import com.example.blooddonor.entity.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmergencyRequestStatusRequest {

    @NotNull
    private RequestStatus status;
}