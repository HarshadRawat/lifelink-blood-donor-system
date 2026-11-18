package com.example.blooddonor.event;

import com.example.blooddonor.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes domain events to RabbitMQ topic exchange strictly after the local
 * database transaction has committed (AFTER_COMMIT), preventing race conditions
 * where consumer threads attempt to query records before MySQL has committed.
 */
@Component
public class EmergencyRequestEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EmergencyRequestEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${lifelink.rabbitmq.exchange:" + RabbitMQConfig.EXCHANGE_NAME + "}")
    private String exchange;

    @Value("${lifelink.rabbitmq.routing-key:" + RabbitMQConfig.ROUTING_KEY + "}")
    private String routingKey;

    public EmergencyRequestEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmergencyRequestCreated(EmergencyRequestCreatedEvent event) {
        try {
            log.info("Transaction committed. Publishing EmergencyRequestCreatedEvent to RabbitMQ [exchange={}, routingKey={}, requestId={}]",
                    exchange, routingKey, event.requestId());
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (AmqpException ex) {
            log.error("Failed to publish EmergencyRequestCreatedEvent for requestId {} to RabbitMQ: {}",
                    event.requestId(), ex.getMessage(), ex);
        }
    }
}
