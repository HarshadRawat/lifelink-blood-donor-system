package com.example.blooddonor.controller;

import com.example.blooddonor.dto.HospitalProfileRequest;
import com.example.blooddonor.dto.HospitalProfileResponse;
import com.example.blooddonor.service.HospitalProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalProfileController {

    private final HospitalProfileService hospitalProfileService;

    public HospitalProfileController(
            HospitalProfileService hospitalProfileService) {

        this.hospitalProfileService = hospitalProfileService;
    }

    @Operation(
            summary = "Create hospital profile",
            description = "Creates a hospital profile for the currently authenticated hospital user. Requires HOSPITAL role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hospital profile created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid profile data"),
            @ApiResponse(responseCode = "403", description = "Access denied - HOSPITAL role required"),
            @ApiResponse(responseCode = "409", description = "Hospital profile already exists")
    })
    @PreAuthorize("hasRole('HOSPITAL')")
    @PostMapping("/profile")
    public HospitalProfileResponse createProfile(
            @Valid @RequestBody HospitalProfileRequest request,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return hospitalProfileService.createProfile(
                request,
                userId
        );
    }

    @Operation(
            summary = "Get hospital profile",
            description = "Returns the profile of the currently authenticated hospital user. Requires HOSPITAL role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hospital profile retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - HOSPITAL role required"),
            @ApiResponse(responseCode = "404", description = "Hospital profile not found")
    })
    @PreAuthorize("hasRole('HOSPITAL')")
    @GetMapping("/profile")
    public HospitalProfileResponse getProfile(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return hospitalProfileService.getProfile(userId);
    }

    @Operation(
            summary = "Update hospital profile",
            description = "Updates the profile of the currently authenticated hospital user. Requires HOSPITAL role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hospital profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid profile data"),
            @ApiResponse(responseCode = "403", description = "Access denied - HOSPITAL role required"),
            @ApiResponse(responseCode = "404", description = "Hospital profile not found")
    })
    @PreAuthorize("hasRole('HOSPITAL')")
    @PutMapping("/profile")
    public HospitalProfileResponse updateProfile(
            @Valid @RequestBody HospitalProfileRequest request,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return hospitalProfileService.updateProfile(
                request,
                userId
        );
    }
}
