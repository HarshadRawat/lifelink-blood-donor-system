package com.example.blooddonor.controller;

import com.example.blooddonor.dto.*;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.entity.Urgency;
import com.example.blooddonor.service.DonorMatchingService;
import com.example.blooddonor.service.EmergencyRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency-requests")
public class EmergencyRequestController {

    private final EmergencyRequestService emergencyRequestService;
    private final DonorMatchingService donorMatchingService;

    public EmergencyRequestController(
            EmergencyRequestService emergencyRequestService,
            DonorMatchingService donorMatchingService) {

        this.emergencyRequestService = emergencyRequestService;
        this.donorMatchingService = donorMatchingService;
    }

    @Operation(
            summary = "Create emergency blood request",
            description = "Creates a new emergency blood request for the currently authenticated hospital. Requires HOSPITAL role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Emergency request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid emergency request data"),
            @ApiResponse(responseCode = "403", description = "Access denied - HOSPITAL role required")
    })
    @PreAuthorize("hasRole('HOSPITAL')")
    @PostMapping
    public EmergencyRequestResponse createRequest(
            @Valid @RequestBody EmergencyRequestRequest request,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return emergencyRequestService.createRequest(
                request,
                userId
        );
    }

    @Operation(
            summary = "Get hospital emergency requests",
            description = "Returns paginated emergency requests created by the currently authenticated hospital. Requests can be filtered by status, blood group, and urgency."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Emergency requests retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied - HOSPITAL role required")
    })
    @PreAuthorize("hasRole('HOSPITAL')")
    @GetMapping
    public Page<EmergencyRequestResponse> getMyRequests(
            Authentication authentication,

            @RequestParam(required = false)
            RequestStatus status,

            @RequestParam(required = false)
            String bloodGroup,

            @RequestParam(required = false)
            Urgency urgency,

            Pageable pageable) {

        Long userId =
                (Long) authentication.getPrincipal();

        return emergencyRequestService.getMyRequests(
                userId,
                status,
                bloodGroup,
                urgency,
                pageable
        );
    }

    @Operation(
            summary = "Update emergency request status",
            description = "Updates the status of an emergency blood request belonging to the currently authenticated hospital."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Emergency request status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "403", description = "Access denied - HOSPITAL role required"),
            @ApiResponse(responseCode = "404", description = "Emergency request not found")
    })
    @PreAuthorize("hasRole('HOSPITAL')")
    @PatchMapping("/{requestId}/status")
    public EmergencyRequestResponse updateStatus(
            @PathVariable Long requestId,
            @Valid @RequestBody EmergencyRequestStatusRequest request,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return emergencyRequestService.updateStatus(
                requestId,
                request,
                userId
        );
    }

    @Operation(
            summary = "Find matching donors",
            description = "Finds compatible and available donors for an emergency request based on blood-group compatibility and geographic distance. Requires HOSPITAL role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching donors found successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - HOSPITAL role required"),
            @ApiResponse(responseCode = "404", description = "Emergency request not found")
    })
    @PreAuthorize("hasRole('HOSPITAL')")
    @PostMapping("/{requestId}/match")
    public List<RequestDonorMatchResponse> findMatches(
            @PathVariable Long requestId) {

        return donorMatchingService.findMatches(requestId);
    }

    @Operation(
            summary = "Accept or reject donor match",
            description = "Allows the matched donor to accept or reject their emergency donation match. Requires DONOR role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid match status"),
            @ApiResponse(responseCode = "403", description = "Access denied - DONOR role required"),
            @ApiResponse(responseCode = "404", description = "Match not found"),
            @ApiResponse(responseCode = "409", description = "Emergency request is no longer active")
    })
    @PreAuthorize("hasRole('DONOR')")
    @PatchMapping("/matches/{matchId}/status")
    public RequestDonorMatchResponse updateMatchStatus(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchStatusRequest request,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return donorMatchingService.updateMatchStatus(
                matchId,
                request,
                userId
        );
    }

    @Operation(
            summary = "Complete donor match",
            description = "Marks an accepted donor match as completed. If the required number of units is fulfilled, the emergency request is marked as fulfilled and remaining matched donors are automatically rejected. Requires HOSPITAL role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donation match completed successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - HOSPITAL role required"),
            @ApiResponse(responseCode = "404", description = "Match not found"),
            @ApiResponse(responseCode = "409", description = "Match cannot be completed because it has not been accepted")
    })
    @PreAuthorize("hasRole('HOSPITAL')")
    @PatchMapping("/matches/{matchId}/complete")
    public RequestDonorMatchResponse completeMatch(
            @PathVariable Long matchId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return donorMatchingService.completeMatch(
                matchId,
                userId
        );
    }

    @Operation(
            summary = "Get matches for emergency request",
            description = "Returns all donor matches associated with an emergency request. Requires HOSPITAL role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request matches retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - HOSPITAL role required"),
            @ApiResponse(responseCode = "404", description = "Emergency request not found")
    })
    @PreAuthorize("hasRole('HOSPITAL')")
    @GetMapping("/{requestId}/matches")
    public List<RequestDonorMatchResponse> getRequestMatches(
            @PathVariable Long requestId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return donorMatchingService.getRequestMatches(requestId, userId);
    }

    @Operation(
            summary = "Get accepted donor contact details",
            description = "Returns contact information for an accepting donor who has granted consent. Only accessible to the hospital owning the emergency request. Requires HOSPITAL role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Donor contact retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or match mismatch"),
            @ApiResponse(responseCode = "403", description = "Access denied - not request owner"),
            @ApiResponse(responseCode = "404", description = "Emergency request or match not found"),
            @ApiResponse(responseCode = "409", description = "Match not accepted or consent not granted")
    })
    @PreAuthorize("hasRole('HOSPITAL')")
    @GetMapping("/{requestId}/matches/{matchId}/contact")
    public DonorContactResponse getDonorContact(
            @PathVariable Long requestId,
            @PathVariable Long matchId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return donorMatchingService.getDonorContact(requestId, matchId, userId);
    }
}
