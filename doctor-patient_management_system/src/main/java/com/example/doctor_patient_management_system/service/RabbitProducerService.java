package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.config.RabbitConfig;
import com.example.doctor_patient_management_system.dto.BookingMessage;
import com.example.doctor_patient_management_system.dto.RegistrationMessage;
import com.example.doctor_patient_management_system.model.MessageOutbox;
import com.example.doctor_patient_management_system.repository.MessageOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RabbitProducerService {

    private static final Logger log = LoggerFactory.getLogger(RabbitProducerService.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessageOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public RabbitProducerService(RabbitTemplate rabbitTemplate,
                                 MessageOutboxRepository outboxRepository,
                                 ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Save message to outbox (called within transaction)
     * This ensures message is saved even if RabbitMQ is down
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveBookingMessage(BookingMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            MessageOutbox outbox = new MessageOutbox(
                    "BOOKING",
                    payload,
                    RabbitConfig.BOOKING_ROUTING_KEY
            );
            outboxRepository.save(outbox);
            log.info("📦 Booking message saved to outbox for appointment ID: {}",
                    message.getAppointmentId());
        } catch (Exception e) {
            log.error("❌ Failed to save booking message to outbox: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save message to outbox", e);
        }
    }

    //Save registration message to outbox
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveRegistrationMessage(RegistrationMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            MessageOutbox outbox = new MessageOutbox(
                    "REGISTRATION",
                    payload,
                    RabbitConfig.REGISTRATION_ROUTING_KEY
            );
            outboxRepository.save(outbox);
            log.info("📦 Registration message saved to outbox for user: {}",
                    message.getUserEmail());
        } catch (Exception e) {
            log.error("❌ Failed to save registration message to outbox: {}",
                    e.getMessage(), e);
            throw new RuntimeException("Failed to save message to outbox", e);
        }
    }

    //Actually send message to RabbitMQ (called by scheduler)
    public boolean sendToQueue(MessageOutbox outbox) {
        try {

            Object messageObject;
            if ("REGISTRATION".equals(outbox.getMessageType())) {
                messageObject = objectMapper.readValue(outbox.getPayload(), RegistrationMessage.class);
            } else if ("BOOKING".equals(outbox.getMessageType())) {
                messageObject = objectMapper.readValue(outbox.getPayload(), BookingMessage.class);
            } else {
                throw new IllegalArgumentException("Unknown message type");
            }

            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE,
                    outbox.getRoutingKey(),
                    messageObject
            );

            outbox.setStatus("SENT");
            outbox.setSentAt(LocalDateTime.now());
            outboxRepository.save(outbox);

            log.info("✅ Message sent successfully: Type={}, ID={}",
                    outbox.getMessageType(), outbox.getId());
            return true;

        } catch (Exception e) {
            outbox.setStatus("FAILED");
            outbox.setRetryCount(outbox.getRetryCount() + 1);
            outbox.setErrorMessage(e.getMessage());
            outboxRepository.save(outbox);

            log.error("❌ Failed to send message: Type={}, ID={}, Error={}",
                    outbox.getMessageType(), outbox.getId(), e.getMessage());
            return false;
        }
    }
}