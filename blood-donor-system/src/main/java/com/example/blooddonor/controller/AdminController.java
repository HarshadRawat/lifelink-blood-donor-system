package com.example.blooddonor.controller;

import com.example.blooddonor.dto.AdminDashboardMetricsResponse;
import com.example.blooddonor.dto.AdminEmergencyRequestSummary;
import com.example.blooddonor.dto.AdminUserSummary;
import com.example.blooddonor.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
