package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.BookingMessage;
import com.example.doctor_patient_management_system.dto.RegistrationMessage;

public interface RabbitConsumerService {

     void consumeBookingMessage(BookingMessage message);

     void consumeRegistrationMessage(RegistrationMessage message);

}
