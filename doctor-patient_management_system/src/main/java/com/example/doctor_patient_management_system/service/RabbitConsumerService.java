package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.config.RabbitConfig;
import com.example.doctor_patient_management_system.dto.BookingMessage;
import com.example.doctor_patient_management_system.dto.RegistrationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class RabbitConsumerService {

    private static final Logger log = LoggerFactory.getLogger(RabbitConsumerService.class);

    //Listen to booking queue and process appointment booking messages
    @RabbitListener(queues = RabbitConfig.BOOKING_QUEUE)
    public void consumeBookingMessage(BookingMessage message) {
        try {
            log.info("📨 ========================================");
            log.info("📨 BOOKING MESSAGE RECEIVED");
            log.info("📨 ========================================");
            log.info("📨 Appointment ID: {}", message.getAppointmentId());
            log.info("📨 Patient: {} ({})", message.getPatientName(), message.getPatientEmail());
            log.info("📨 Doctor: {} ({})", message.getDoctorName(), message.getDoctorEmail());
            log.info("📨 Date: {}", message.getAppointmentDate());
            log.info("📨 Time: {}", message.getAppointmentTime());
            log.info("📨 Speciality: {}", message.getSpeciality());
            log.info("📨 Fee: {} BDT", message.getConsultationFee());
            log.info("📨 ========================================");

            // Process booking tasks
            sendBookingConfirmationEmail(message);
            sendDoctorNotificationEmail(message);
            logBookingAnalytics(message);

            log.info("✅ Booking message processed successfully");
        } catch (Exception e) {
            log.error("❌ Error processing booking message: {}", e.getMessage(), e);
        }
    }

    //Listen to registration queue and process user registration messages
    @RabbitListener(queues = RabbitConfig.REGISTRATION_QUEUE)
    public void consumeRegistrationMessage(RegistrationMessage message) {
        try {
            log.info("📨 ========================================");
            log.info("📨 REGISTRATION MESSAGE RECEIVED");
            log.info("📨 ========================================");
            log.info("📨 Email: {}", message.getUserEmail());
            log.info("📨 Role: {}", message.getRole());
            log.info("📨 Name: {}", message.getFullName());
            log.info("📨 ========================================");

            // Process registration based on role
            if ("DOCTOR".equals(message.getRole())) {
                sendDoctorWelcomeEmail(message);
                notifyAdminNewDoctorRegistration(message);
            } else if ("PATIENT".equals(message.getRole())) {
                sendPatientWelcomeEmail(message);
            }

            // Common tasks for all registrations
            logRegistrationAnalytics(message);

            log.info("✅ Registration message processed successfully");
        } catch (Exception e) {
            log.error("❌ Error processing registration message: {}", e.getMessage(), e);
        }
    }

    // =============== BOOKING EMAIL METHODS ===============

    //Send booking confirmation email to patient
    private void sendBookingConfirmationEmail(BookingMessage message) {
        log.info("📧 Sending booking confirmation email to patient: {}", message.getPatientEmail());

        // TODO: Implement actual email sending
        // Example email content:
        String emailBody = String.format("""
            Dear %s,
            
            Your appointment has been confirmed!
            
            Doctor: Dr. %s
            Speciality: %s
            Date: %s
            Time: %s
            Consultation Fee: %d BDT
            
            Please arrive 10 minutes before your appointment time.
            
            Thank you for choosing our service!
            """,
                message.getPatientName(),
                message.getDoctorName(),
                message.getSpeciality(),
                message.getAppointmentDate(),
                message.getAppointmentTime(),
                message.getConsultationFee()
        );

        log.info("📧 Email body prepared: \n{}", emailBody);
    }

    //Send notification to doctor about new booking
    private void sendDoctorNotificationEmail(BookingMessage message) {
        log.info("📧 Sending notification email to doctor: {}", message.getDoctorEmail());

        String emailBody = String.format("""
            Dear Dr. %s,
            
            You have a new appointment booking!
            
            Patient: %s
            Date: %s
            Time: %s
            
            Please check your dashboard for more details.
            """,
                message.getDoctorName(),
                message.getPatientName(),
                message.getAppointmentDate(),
                message.getAppointmentTime()
        );

        log.info("📧 Doctor notification prepared");
    }

    // =============== REGISTRATION EMAIL METHODS ===============

    //Send welcome email to newly registered doctor
    private void sendDoctorWelcomeEmail(RegistrationMessage message) {
        log.info("📧 Sending welcome email to doctor: {}", message.getUserEmail());

        String emailBody = String.format("""
            Dear Dr. %s,
            
            Welcome to our Doctor-Patient Management System!
            
            Your account has been successfully created. Please complete your profile 
            to start accepting appointments from patients.
            
            Profile Setup Steps:
            1. Add your specialization
            2. Set consultation hours
            3. Add consultation fee
            4. Set your off days
            
            Thank you for joining us!
            
            Best regards,
            Admin Team
            """,
                message.getFullName()
        );

        log.info("📧 Doctor welcome email prepared");
    }

    //Send welcome email to newly registered patient
    private void sendPatientWelcomeEmail(RegistrationMessage message) {
        log.info("📧 Sending welcome email to patient: {}", message.getUserEmail());

        String emailBody = String.format("""
            Dear %s,
            
            Welcome to our Doctor-Patient Management System!
            
            Your account has been successfully created. You can now:
            - Search for doctors by specialization
            - Book appointments
            - View your appointment history
            - Download prescriptions
            
            Please complete your profile to get started.
            
            Thank you for choosing our service!
            
            Best regards,
            Admin Team
            """,
                message.getFullName()
        );

        log.info("📧 Patient welcome email prepared");
    }

    /**
     * Notify admin about new doctor registration
     */
    private void notifyAdminNewDoctorRegistration(RegistrationMessage message) {
        log.info("📧 Notifying admin about new doctor registration: {}", message.getUserEmail());
        log.info("   Doctor Name: {}", message.getFullName());
    }

    // =============== ANALYTICS METHODS ===============

    /**
     * Log booking details for analytics
     */
    private void logBookingAnalytics(BookingMessage message) {
        log.info("📊 Logging booking analytics...");
        log.info("   Appointment ID: {}", message.getAppointmentId());
        log.info("   Doctor Speciality: {}", message.getSpeciality());
        log.info("   Booking Date: {}", message.getAppointmentDate());
        log.info("   Revenue: {} BDT", message.getConsultationFee());

        // TODO: Save to analytics database
        // analyticsService.logBooking(message);
    }

    /**
     * Log registration analytics
     */
    private void logRegistrationAnalytics(RegistrationMessage message) {
        log.info("📊 Logging registration analytics...");
        log.info("   User Email: {}", message.getUserEmail());
        log.info("   User Role: {}", message.getRole());
        log.info("   Registration Time: {}", java.time.LocalDateTime.now());

        // TODO: Save to analytics database
        // analyticsService.logRegistration(message);
    }
}