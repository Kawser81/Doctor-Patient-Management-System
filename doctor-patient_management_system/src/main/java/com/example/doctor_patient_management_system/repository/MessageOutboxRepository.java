package com.example.doctor_patient_management_system.repository;

import com.example.doctor_patient_management_system.model.MessageOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageOutboxRepository extends JpaRepository<MessageOutbox, Long> {
    List<MessageOutbox> findByStatusAndRetryCountLessThan(String status, Integer maxRetries);
}