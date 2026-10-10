package com.example.blooddonor.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DonorProfileResponse {

    private Long id;
    private Long userId;

    private String bloodGroup;
    private String address;
    private String city;

    private Double latitude;
    private Double longitude;

    private String contactNumber;
    private boolean available;
}