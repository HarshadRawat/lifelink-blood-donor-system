package com.example.blooddonor.controller;

import com.example.blooddonor.dto.AvailabilityRequest;
import com.example.blooddonor.dto.DonorProfileRequest;
import com.example.blooddonor.dto.DonorProfileResponse;
import com.example.blooddonor.dto.RequestDonorMatchResponse;
import com.example.blooddonor.service.DonorMatchingService;
import com.example.blooddonor.service.DonorProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donors")
public class DonorProfileController {

    private final DonorProfileService donorProfileService;
    private final DonorMatchingService donorMatchingService;

    public DonorProfileController(
            DonorProfileService donorProfileService,
            DonorMatchingService donorMatchingService) {

        this.donorProfileService = donorProfileService;
        this.donorMatchingService = donorMatchingService;
    }

    @Operation(
            summary = "Create donor profile",
            description = "Creates a donor profile for the currently authenticated donor. Requires DONOR role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donor profile created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid profile data"),
            @ApiResponse(responseCode = "403", description = "Access denied - DONOR role required"),
            @ApiResponse(responseCode = "409", description = "Donor profile already exists")
    })
    @PreAuthorize("hasRole('DONOR')")
    @PostMapping("/profile")
    public DonorProfileResponse createProfile(
            @Valid @RequestBody DonorProfileRequest request,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return donorProfileService.createProfile(
                request,
                userId
        );
    }

    @Operation(
            summary = "Update donor profile",
            description = "Updates the profile of the currently authenticated donor. Requires DONOR role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donor profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid profile data"),
            @ApiResponse(responseCode = "403", description = "Access denied - DONOR role required"),
            @ApiResponse(responseCode = "404", description = "Donor profile not found")
    })
    @PreAuthorize("hasRole('DONOR')")
    @PutMapping("/profile")
    public DonorProfileResponse updateProfile(
            @Valid @RequestBody DonorProfileRequest request,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return donorProfileService.updateProfile(
                request,
                userId
        );
    }

    @Operation(
            summary = "Get donor profile",
            description = "Returns the profile of the currently authenticated donor. Requires DONOR role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donor profile retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - DONOR role required"),
            @ApiResponse(responseCode = "404", description = "Donor profile not found")
    })
    @PreAuthorize("hasRole('DONOR')")
    @GetMapping("/profile")
    public DonorProfileResponse getProfile(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return donorProfileService.getProfile(userId);
    }

    @Operation(
            summary = "Update donor availability",
            description = "Updates whether the currently authenticated donor is available to receive emergency blood donation requests. Requires DONOR role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donor availability updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid availability data"),
            @ApiResponse(responseCode = "403", description = "Access denied - DONOR role required"),
            @ApiResponse(responseCode = "404", description = "Donor profile not found")
    })
    @PreAuthorize("hasRole('DONOR')")
    @PatchMapping("/profile/availability")
    public DonorProfileResponse updateAvailability(
            @Valid @RequestBody AvailabilityRequest request,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return donorProfileService.updateAvailability(
                request,
                userId
        );
    }

    @Operation(
            summary = "Get donor matches",
            description = "Returns emergency blood donation requests matched with the currently authenticated donor. Requires DONOR role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donor matches retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - DONOR role required")
    })
    @PreAuthorize("hasRole('DONOR')")
    @GetMapping("/matches")
    public List<RequestDonorMatchResponse> getMyMatches(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return donorMatchingService.getMyMatches(userId);
    }
}
