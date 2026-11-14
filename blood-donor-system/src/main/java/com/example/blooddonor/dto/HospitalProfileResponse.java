package com.example.blooddonor.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HospitalProfileResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;

    private String hospitalName;
    private String registrationNumber;
    private String address;
    private String city;

    private Double latitude;
    private Double longitude;

    private String contactNumber;
}