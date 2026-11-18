package com.example.blooddonor.controller;

import com.example.blooddonor.dto.AdminDashboardMetricsResponse;
import com.example.blooddonor.dto.AdminEmergencyRequestSummary;
import com.example.blooddonor.dto.AdminUserSummary;
import com.example.blooddonor.dto.HospitalProfileResponse;
import com.example.blooddonor.dto.HospitalVerificationRequest;
import com.example.blooddonor.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Oversight", description = "Endpoints for platform monitoring and administrative visibility")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "Get high-level platform health & activity metrics")
    @ApiResponse(responseCode = "200", description = "Platform metrics retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden for non-admin accounts")
    @GetMapping("/metrics")
    public AdminDashboardMetricsResponse getMetrics() {
        return adminService.getMetrics();
    }

    @Operation(summary = "Get paginated system-wide emergency requests")
    @ApiResponse(responseCode = "200", description = "Emergency requests list retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden for non-admin accounts")
    @GetMapping("/requests")
    public Page<AdminEmergencyRequestSummary> getRecentRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminService.getRecentEmergencyRequests(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    @Operation(summary = "Get paginated registered platform users")
    @ApiResponse(responseCode = "200", description = "Users directory retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden for non-admin accounts")
    @GetMapping("/users")
    public Page<AdminUserSummary> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminService.getUsers(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        );
    }

    @Operation(summary = "Get paginated pending (unverified) hospitals")
    @ApiResponse(responseCode = "200", description = "Pending hospitals list retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden for non-admin accounts")
    @GetMapping("/hospitals/pending")
    public Page<HospitalProfileResponse> getPendingHospitals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminService.getPendingHospitals(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        );
    }

    @Operation(summary = "Verify or reject a hospital registration")
    @ApiResponse(responseCode = "200", description = "Hospital verification status updated successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden for non-admin accounts")
    @ApiResponse(responseCode = "400", description = "Invalid request payload or hospital not found")
    @PatchMapping("/hospitals/{id}/verify")
    public HospitalProfileResponse verifyHospital(
            @PathVariable Long id,
            @Valid @RequestBody HospitalVerificationRequest request
    ) {
        return adminService.verifyHospital(id, request.getVerified());
    }
}
