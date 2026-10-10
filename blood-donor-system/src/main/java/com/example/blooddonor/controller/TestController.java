package com.example.blooddonor.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/protected")
    public String protectedEndpoint() {
        return "JWT authentication is working";
    }

    @GetMapping("/donor-only")
    @PreAuthorize("hasRole('DONOR')")
    public String donorOnly() {
        return "DONOR access granted";
    }
}