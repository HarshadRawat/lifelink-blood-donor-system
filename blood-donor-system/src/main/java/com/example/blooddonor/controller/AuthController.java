package com.example.blooddonor.controller;

import com.example.blooddonor.dto.LoginRequest;
import com.example.blooddonor.dto.LoginResponse;
import com.example.blooddonor.dto.RegisterRequest;
import com.example.blooddonor.dto.UserResponse;
import com.example.blooddonor.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates a new donor or hospital user account."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Admin registration is not allowed"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping("/register")
    public UserResponse register(
            @Valid @RequestBody RegisterRequest request) {

        return userService.register(request);
    }

    @Operation(
            summary = "Login user",
            description = "Authenticates a user and returns a JWT token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request) {

        return userService.login(request);
    }

    @Operation(
            summary = "Refresh access token",
            description = "Exchanges a valid refresh token for a new short-lived access token and rotated refresh token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Expired or compromised refresh token")
    })
    @PostMapping("/refresh")
    public LoginResponse refresh(
            @Valid @RequestBody com.example.blooddonor.dto.RefreshTokenRequest request) {

        return userService.refresh(request);
    }

    @Operation(
            summary = "Logout user",
            description = "Revokes the active refresh token and invalidates the session."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out successfully")
    })
    @PostMapping("/logout")
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> logout(
            @RequestBody(required = false) com.example.blooddonor.dto.RefreshTokenRequest request) {

        userService.logout(request);
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of("message", "Logged out successfully"));
    }
}