package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.BookingMessage;
import com.example.doctor_patient_management_system.dto.RegistrationMessage;
import com.example.doctor_patient_management_system.model.MessageOutbox;

public interface RabbitProducerService {

     void saveBookingMessage(BookingMessage message);

     void saveRegistrationMessage(RegistrationMessage message);

     boolean sendToQueue(MessageOutbox outbox);

}
