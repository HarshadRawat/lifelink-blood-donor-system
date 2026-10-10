package com.example.blooddonor.exception;

public class NotificationAccessDeniedException
        extends RuntimeException {

    public NotificationAccessDeniedException(String message) {
        super(message);
    }
}