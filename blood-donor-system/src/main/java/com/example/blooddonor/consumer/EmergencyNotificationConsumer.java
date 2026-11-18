package com.example.blooddonor.consumer;

import com.example.blooddonor.config.RabbitMQConfig;
import com.example.blooddonor.event.EmergencyRequestCreatedEvent;
import com.example.blooddonor.service.DonorMatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Asynchronous RabbitMQ Consumer for Emergency Request Created Events (Task 7).
 * Listens on notifications.q, executes donor matching and WebSocket push notifications,
 * and relies on Dead-Letter Exchange (DLX) routing to notifications.dlq on persistent failure.
 */
@Component
public class EmergencyNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmergencyNotificationConsumer.class);

    private final DonorMatchingService donorMatchingService;

    public EmergencyNotificationConsumer(DonorMatchingService donorMatchingService) {
        this.donorMatchingService = donorMatchingService;
    }

    @RabbitListener(queues = "${lifelink.rabbitmq.queue:" + RabbitMQConfig.QUEUE_NAME + "}")
    public void handleEmergencyRequestCreated(EmergencyRequestCreatedEvent event) {
        log.info("Processing asynchronous emergency notification for requestId: {}, bloodGroup: {}",
                event.requestId(), event.bloodGroup());

        try {
            donorMatchingService.findMatches(event.requestId());
            log.info("Successfully matched and notified donors for requestId: {}", event.requestId());
        } catch (Exception ex) {
            log.error("Failed to process emergency notification for requestId {}: {}",
                    event.requestId(), ex.getMessage(), ex);
            // Re-throw to activate Spring AMQP listener retry and DLQ routing
            throw ex;
        }
    }
}
