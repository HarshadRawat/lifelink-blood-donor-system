package com.example.blooddonor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise RabbitMQ AMQP Topology Configuration (Task 7):
 * - Main Exchange: lifelink.events (TopicExchange)
 * - Work Queue: notifications.q (with DLX fallback arguments)
 * - Dead-Letter Exchange: lifelink.events.dlx (DirectExchange)
 * - Dead-Letter Queue: notifications.dlq (holds unprocessable / failed messages)
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "lifelink.events";
    public static final String QUEUE_NAME = "notifications.q";
    public static final String ROUTING_KEY = "emergency.request.created";

    public static final String DLX_EXCHANGE_NAME = "lifelink.events.dlx";
    public static final String DLQ_QUEUE_NAME = "notifications.dlq";
    public static final String DLQ_ROUTING_KEY = "notifications.dlq";

    @Bean
    public TopicExchange eventsExchange(
            @Value("${lifelink.rabbitmq.exchange:" + EXCHANGE_NAME + "}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange(
            @Value("${lifelink.rabbitmq.dlx:" + DLX_EXCHANGE_NAME + "}") String dlxName) {
        return new DirectExchange(dlxName, true, false);
    }

    @Bean
    public Queue deadLetterQueue(
            @Value("${lifelink.rabbitmq.dlq:" + DLQ_QUEUE_NAME + "}") String dlqName) {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Binding deadLetterBinding(
            Queue deadLetterQueue,
            DirectExchange deadLetterExchange,
            @Value("${lifelink.rabbitmq.dlq-routing-key:" + DLQ_ROUTING_KEY + "}") String dlqRoutingKey) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(dlqRoutingKey);
    }

    @Bean
    public Queue notificationsQueue(
            @Value("${lifelink.rabbitmq.queue:" + QUEUE_NAME + "}") String queueName,
            @Value("${lifelink.rabbitmq.dlx:" + DLX_EXCHANGE_NAME + "}") String dlxName,
            @Value("${lifelink.rabbitmq.dlq-routing-key:" + DLQ_ROUTING_KEY + "}") String dlqRoutingKey) {

        Map<String, Object> arguments = new HashMap<>();
        // Route rejected or retry-exhausted messages to DLX
        arguments.put("x-dead-letter-exchange", dlxName);
        arguments.put("x-dead-letter-routing-key", dlqRoutingKey);

        return QueueBuilder.durable(queueName)
                .withArguments(arguments)
                .build();
    }

    @Bean
    public Binding notificationsBinding(
            Queue notificationsQueue,
            TopicExchange eventsExchange,
            @Value("${lifelink.rabbitmq.routing-key:" + ROUTING_KEY + "}") String routingKey) {
        return BindingBuilder.bind(notificationsQueue)
                .to(eventsExchange)
                .with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
