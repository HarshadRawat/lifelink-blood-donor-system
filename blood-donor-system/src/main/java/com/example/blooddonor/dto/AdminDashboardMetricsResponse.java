package com.example.blooddonor.dto;

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
public class AdminDashboardMetricsResponse {
    private long totalUsers;
    private long totalDonors;
    private long totalHospitals;
    private long totalEmergencyRequests;
    private long activeEmergencyRequests;
    private long fulfilledEmergencyRequests;
    private long completedDonations;
}
