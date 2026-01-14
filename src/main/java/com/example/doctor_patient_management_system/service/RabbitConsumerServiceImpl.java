package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.config.RabbitConfig;
import com.example.doctor_patient_management_system.dto.BookingMessage;
import com.example.doctor_patient_management_system.dto.RegistrationMessage;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class RabbitConsumerServiceImpl implements RabbitConsumerService {

    private static final Logger log = LoggerFactory.getLogger(RabbitConsumerServiceImpl.class);

    @Override
    @RabbitListener(queues = RabbitConfig.BOOKING_QUEUE)
    public void consumeBookingMessage(BookingMessage message) {
        try {
            log.info("Booking message received for appointment ID: {}", message.getAppointmentId());

            sendBookingConfirmationEmail(message);
            sendDoctorNotificationEmail(message);
            logBookingAnalytics(message);

            log.info("Booking message processed successfully");
        } catch (Exception e) {
            log.error("Error processing booking message: {}", e.getMessage(), e);
        }
    }

    @Override
    @RabbitListener(queues = RabbitConfig.REGISTRATION_QUEUE)
    public void consumeRegistrationMessage(RegistrationMessage message) {
        try {
            log.info("Registration message received for email: {}", message.getUserEmail());

            if (Role.DOCTOR.equals(message.getRole())) {
                sendDoctorWelcomeEmail(message);
                notifyAdminNewDoctorRegistration(message);
            } else if (Role.PATIENT.equals(message.getRole())) {
                sendPatientWelcomeEmail(message);
            }

            logRegistrationAnalytics(message);

            log.info("Registration message processed successfully");
        } catch (Exception e) {
            log.error("Error processing registration message: {}", e.getMessage(), e);
        }
    }

    private void sendBookingConfirmationEmail(BookingMessage message) {
        log.info("Sending booking confirmation email to patient: {}", message.getPatientEmail());
    }

    private void sendDoctorNotificationEmail(BookingMessage message) {
        log.info("Sending notification email to doctor: {}", message.getDoctorEmail());
    }

    private void sendDoctorWelcomeEmail(RegistrationMessage message) {
        log.info("Sending welcome email to doctor: {}", message.getUserEmail());
    }

    private void sendPatientWelcomeEmail(RegistrationMessage message) {
        log.info("Sending welcome email to patient: {}", message.getUserEmail());
    }

    private void notifyAdminNewDoctorRegistration(RegistrationMessage message) {
        log.info("Notifying admin about new doctor registration: {}", message.getUserEmail());
    }

    private void logBookingAnalytics(BookingMessage message) {
        log.info("Logging booking analytics for appointment ID: {}", message.getAppointmentId());
    }

    private void logRegistrationAnalytics(RegistrationMessage message) {
        log.info("Logging registration analytics for user: {}", message.getUserEmail());
    }
}