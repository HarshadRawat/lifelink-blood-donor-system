package com.example.blooddonor.exception;

public class AdminRegistrationNotAllowedException extends RuntimeException {

    public AdminRegistrationNotAllowedException(String message) {
        super(message);
    }
}