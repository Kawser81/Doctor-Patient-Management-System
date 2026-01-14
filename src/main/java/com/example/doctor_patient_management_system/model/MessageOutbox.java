package com.example.doctor_patient_management_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_outbox")
public class MessageOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String messageType; // "REGISTRATION" or "BOOKING"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON string

    @Column(nullable = false)
    private String routingKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private String status; // PENDING, SENT, FAILED

    @Column
    private Integer retryCount = 0;

    @Column
    private String errorMessage;

    // Constructors, Getters, Setters
    public MessageOutbox() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    public MessageOutbox(String messageType, String payload, String routingKey) {
        this();
        this.messageType = messageType;
        this.payload = payload;
        this.routingKey = routingKey;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}