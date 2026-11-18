package com.example.blooddonor.event;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Domain event published when an emergency blood request is created.
 * Dispatched asynchronously to RabbitMQ exchange (lifelink.events)
 * after the local database transaction successfully commits.
 */
public record EmergencyRequestCreatedEvent(
        Long requestId,
        String bloodGroup,
        Integer unitsRequired,
        String urgency,
        Double hospitalLatitude,
        Double hospitalLongitude,
        Long hospitalId,
        String hospitalName,
        LocalDateTime createdAt
) implements Serializable {}
