package com.example.blooddonor.dto;

import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.entity.Urgency;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class EmergencyRequestResponse {

    private Long id;

    private String bloodGroup;

    private Integer unitsRequired;

    private Urgency urgency;

    private RequestStatus status;

    private LocalDateTime createdAt;

    private Long hospitalId;

    private String hospitalName;
}