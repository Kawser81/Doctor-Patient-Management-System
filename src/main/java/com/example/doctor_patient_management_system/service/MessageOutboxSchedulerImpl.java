package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.model.MessageOutbox;
import com.example.doctor_patient_management_system.repository.MessageOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageOutboxSchedulerImpl implements MessageOutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(MessageOutboxSchedulerImpl.class);
    private static final int MAX_RETRIES = 5;

    private final MessageOutboxRepository outboxRepository;
    private final RabbitProducerServiceImpl producerService;

    public MessageOutboxSchedulerImpl(MessageOutboxRepository outboxRepository,
                                      RabbitProducerServiceImpl producerService) {
        this.outboxRepository = outboxRepository;
        this.producerService = producerService;
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void processPendingMessages() {
        try {
            List<MessageOutbox> pendingMessages =
                    outboxRepository.findByStatusAndRetryCountLessThan("PENDING", MAX_RETRIES);

            if (pendingMessages.isEmpty()) {
                return;
            }

            log.info(" Processing {} pending messages...", pendingMessages.size());

            for (MessageOutbox message : pendingMessages) {
                boolean sent = producerService.sendToQueue(message);
                if (sent) {
                    log.info(" Message sent: ID={}, Type={}",
                            message.getId(), message.getMessageType());
                } else {
                    log.warn(" Message failed: ID={}, Retry={}/{}",
                            message.getId(), message.getRetryCount(), MAX_RETRIES);
                }
            }

        } catch (Exception e) {
            log.error(" Error processing outbox messages: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 15000)
    public void retryFailedMessages() {
        try {
            List<MessageOutbox> failedMessages =
                    outboxRepository.findByStatusAndRetryCountLessThan("FAILED", MAX_RETRIES);

            if (failedMessages.isEmpty()) {
                return;
            }

            log.info(" Retrying {} failed messages...", failedMessages.size());

            for (MessageOutbox message : failedMessages) {
                boolean sent = producerService.sendToQueue(message);
                if (sent) {
                    log.info(" Failed message recovered: ID={}", message.getId());
                }
            }

        } catch (Exception e) {
            log.error(" Error retrying failed messages: {}", e.getMessage(), e);
        }
    }
}
