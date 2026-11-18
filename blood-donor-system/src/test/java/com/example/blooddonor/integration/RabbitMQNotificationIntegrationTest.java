package com.example.blooddonor.integration;

import com.example.blooddonor.config.RabbitMQConfig;
import com.example.blooddonor.event.EmergencyRequestCreatedEvent;
import com.example.blooddonor.service.DonorMatchingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * End-to-End Containerized RabbitMQ Integration Test (Task 7):
 * - Spins up real RabbitMQ 3.x instance via Testcontainers.
 * - Verifies asynchronous publishing to lifelink.events.
 * - Verifies asynchronous consumer execution on notifications.q.
 * - Verifies Dead-Letter Queue (notifications.dlq) routing upon retry exhaustion.
 * - Skips gracefully in environments without a Docker daemon via disabledWithoutDocker = true.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class RabbitMQNotificationIntegrationTest {

    @Container
    private static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @DynamicPropertySource
    static void configureRabbit(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "true");
        // Fast test retries: 100ms initial interval, max 2 attempts
        registry.add("spring.rabbitmq.listener.simple.retry.enabled", () -> "true");
        registry.add("spring.rabbitmq.listener.simple.retry.initial-interval", () -> "100ms");
        registry.add("spring.rabbitmq.listener.simple.retry.max-attempts", () -> "2");
        registry.add("spring.rabbitmq.listener.simple.retry.multiplier", () -> "1.5");
        registry.add("spring.rabbitmq.listener.simple.default-requeue-rejected", () -> "false");
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @MockitoBean
    private DonorMatchingService donorMatchingService;

    @Test
    @DisplayName("Should publish event to exchange and asynchronously trigger consumer from notifications.q")
    void shouldPublishAndConsumeAsynchronously() {
        EmergencyRequestCreatedEvent event = new EmergencyRequestCreatedEvent(
                999L,
                "O+",
                2,
                "HIGH",
                12.9716,
                77.5946,
                1L,
                "Bangalore General Hospital",
                LocalDateTime.now()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                event
        );

        // Await consumer execution asynchronously within 5 seconds
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(donorMatchingService, atLeastOnce()).findMatches(999L);
        });
    }

    @Test
    @DisplayName("Should route unprocessable message to notifications.dlq when consumer exhausts retries")
    void shouldRouteToDeadLetterQueueOnFailure() {
        // Purge DLQ prior to verification to ensure clean state
        amqpAdmin.purgeQueue(RabbitMQConfig.DLQ_QUEUE_NAME, false);

        EmergencyRequestCreatedEvent failingEvent = new EmergencyRequestCreatedEvent(
                888L,
                "AB-",
                1,
                "CRITICAL",
                12.9716,
                77.5946,
                1L,
                "Bangalore General Hospital",
                LocalDateTime.now()
        );

        // Simulate repeated unrecoverable service failure
        doThrow(new RuntimeException("Simulated matching failure"))
                .when(donorMatchingService)
                .findMatches(888L);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                failingEvent
        );

        // Await message routing to dead-letter queue (notifications.dlq) after retries exhaust
        await().atMost(Duration.ofSeconds(6)).untilAsserted(() -> {
            EmergencyRequestCreatedEvent deadLetter = (EmergencyRequestCreatedEvent) rabbitTemplate
                    .receiveAndConvert(RabbitMQConfig.DLQ_QUEUE_NAME, 1000);
            assertNotNull(deadLetter, "Dead-letter message should be present in notifications.dlq");
            assertEquals(888L, deadLetter.requestId());
            assertEquals("AB-", deadLetter.bloodGroup());
        });
    }
}
