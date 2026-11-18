package com.example.blooddonor.consumer;

import com.example.blooddonor.event.EmergencyRequestCreatedEvent;
import com.example.blooddonor.service.DonorMatchingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyNotificationConsumerTest {

    @Mock
    private DonorMatchingService donorMatchingService;

    @InjectMocks
    private EmergencyNotificationConsumer consumer;

    @Test
    @DisplayName("Should invoke donor matching service upon receiving emergency request event")
    void shouldProcessEmergencyRequestCreatedSuccessfully() {
        EmergencyRequestCreatedEvent event = new EmergencyRequestCreatedEvent(
                200L,
                "B+",
                2,
                "MEDIUM",
                12.9716,
                77.5946,
                10L,
                "Apex Clinic",
                LocalDateTime.now()
        );

        consumer.handleEmergencyRequestCreated(event);

        verify(donorMatchingService, times(1)).findMatches(200L);
    }

    @Test
    @DisplayName("Should rethrow exception to trigger Spring AMQP listener retry and DLQ routing")
    void shouldRethrowExceptionOnFailure() {
        EmergencyRequestCreatedEvent event = new EmergencyRequestCreatedEvent(
                201L,
                "AB+",
                1,
                "LOW",
                12.9716,
                77.5946,
                10L,
                "Apex Clinic",
                LocalDateTime.now()
        );

        doThrow(new RuntimeException("Database timeout during matching"))
                .when(donorMatchingService)
                .findMatches(201L);

        assertThrows(RuntimeException.class, () -> consumer.handleEmergencyRequestCreated(event));
        verify(donorMatchingService, times(1)).findMatches(201L);
    }
}
