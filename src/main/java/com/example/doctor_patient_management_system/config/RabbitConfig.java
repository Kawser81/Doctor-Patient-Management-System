package com.example.doctor_patient_management_system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Queue names
    public static final String BOOKING_QUEUE = "booking.queue";
    public static final String REGISTRATION_QUEUE = "registration.queue";

    // Exchange name
    public static final String EXCHANGE = "doctor.patient.exchange";

    // Routing keys
    public static final String BOOKING_ROUTING_KEY = "booking.key";
    public static final String REGISTRATION_ROUTING_KEY = "registration.key";

    // Declare Booking Queue
    @Bean
    public Queue bookingQueue() {
        return new Queue(BOOKING_QUEUE, true);
    }

    // Declare Registration Queue
    @Bean
    public Queue registrationQueue() {
        return new Queue(REGISTRATION_QUEUE, true);
    }

    // Declare Exchange
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    // Bind Booking Queue to Exchange
    @Bean
    public Binding bookingBinding(Queue bookingQueue, TopicExchange exchange) {
        return BindingBuilder
                .bind(bookingQueue)
                .to(exchange)
                .with(BOOKING_ROUTING_KEY);
    }

    // Bind Registration Queue to Exchange
    @Bean
    public Binding registrationBinding(Queue registrationQueue, TopicExchange exchange) {
        return BindingBuilder
                .bind(registrationQueue)
                .to(exchange)
                .with(REGISTRATION_ROUTING_KEY);
    }

    // JSON Message Converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Register JavaTimeModule for LocalDate/LocalDateTime
        objectMapper.registerModule(new JavaTimeModule());

        // Write dates as strings, not timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // CRITICAL: Disable default typing to avoid @class requirement
        objectMapper.deactivateDefaultTyping();

        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // RabbitTemplate with JSON converter
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
