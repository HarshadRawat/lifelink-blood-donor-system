package com.example.blooddonor.event;

import com.example.blooddonor.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyRequestEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private EmergencyRequestEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new EmergencyRequestEventPublisher(rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "exchange", RabbitMQConfig.EXCHANGE_NAME);
        ReflectionTestUtils.setField(publisher, "routingKey", RabbitMQConfig.ROUTING_KEY);
    }

    @Test
    @DisplayName("Should convert and publish event to RabbitMQ after transaction commit")
    void shouldPublishEventToRabbitMQ() {
        EmergencyRequestCreatedEvent event = new EmergencyRequestCreatedEvent(
                100L,
                "A+",
                3,
                "HIGH",
                12.9716,
                77.5946,
                5L,
                "City Hospital",
                LocalDateTime.now()
        );

        publisher.onEmergencyRequestCreated(event);

        ArgumentCaptor<EmergencyRequestCreatedEvent> captor = ArgumentCaptor.forClass(EmergencyRequestCreatedEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY),
                captor.capture()
        );

        EmergencyRequestCreatedEvent captured = captor.getValue();
        assertEquals(100L, captured.requestId());
        assertEquals("A+", captured.bloodGroup());
        assertEquals(3, captured.unitsRequired());
        assertEquals("City Hospital", captured.hospitalName());
    }

    @Test
    @DisplayName("Should handle AmqpException gracefully without propagating error to transaction")
    void shouldCatchAmqpExceptionGracefully() {
        EmergencyRequestCreatedEvent event = new EmergencyRequestCreatedEvent(
                101L,
                "O-",
                1,
                "CRITICAL",
                12.9716,
                77.5946,
                5L,
                "City Hospital",
                LocalDateTime.now()
        );

        doThrow(new AmqpException("Broker unreachable"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> publisher.onEmergencyRequestCreated(event));
    }
}
