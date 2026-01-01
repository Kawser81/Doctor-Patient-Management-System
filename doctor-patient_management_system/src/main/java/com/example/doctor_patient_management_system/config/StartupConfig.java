package com.example.doctor_patient_management_system.config;

import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;


@Configuration
public class StartupConfig {

    private static final Logger logger = LoggerFactory.getLogger(StartupConfig.class);

    @Bean
    @Transactional
    public CommandLineRunner autoCancelPastAppointmentsOnStartup(
            AppointmentRepository appointmentRepository) {

        return args -> {
            try {
                LocalDate today = LocalDate.now();

                logger.info("===========================================");
                logger.info("🚀 Application Startup: Checking for past appointments...");
                logger.info("Current Date: {}", today);
                logger.info("Will cancel appointments BEFORE: {}", today);

                // Past date এর সব CONFIRMED appointments খুঁজে বের করুন
                // তবে আজকের date এর appointments cancel হবে না
                List<Appointment> pastAppointments = appointmentRepository
                        .findAll()
                        .stream()
                        .filter(a -> a.getAppointmentDate() != null)
                        .filter(a -> a.getAppointmentDate().isBefore(today))  // 🔥 Strictly before today (not equal)
                        .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                        .filter(a -> a.getPrescription() == null)  // Prescription নেই যেগুলোতে
                        .collect(Collectors.toList());

                if (pastAppointments.isEmpty()) {
                    logger.info("✅ No past appointments without prescription found to cancel");
                    logger.info("===========================================");
                    return;
                }

                logger.info("📋 Found {} past appointments (without prescription) to cancel:",
                        pastAppointments.size());

                // Status update করুন এবং details log করুন
                for (Appointment appointment : pastAppointments) {
                    logger.info("   - Appointment ID: {}, Date: {} (expired), Doctor ID: {}, Patient ID: {} [No Prescription]",
                            appointment.getId(),
                            appointment.getAppointmentDate(),
                            appointment.getDoctor().getId(),
                            appointment.getPatient().getId());

                    appointment.setStatus(AppointmentStatus.CANCELLED);
                }

                // Batch save - efficient
                appointmentRepository.saveAll(pastAppointments);

                logger.info("✅ Successfully cancelled {} past appointments (without prescription)",
                        pastAppointments.size());
                logger.info("ℹ️  Note: Today's appointments ({}) are NOT cancelled", today);
                logger.info("===========================================");

            } catch (Exception e) {
                logger.error("❌ Error while cancelling past appointments on startup: {}",
                        e.getMessage(), e);
                logger.error("===========================================");
            }
        };
    }
}









//package com.example.doctor_patient_management_system.config;
//
//import com.example.doctor_patient_management_system.model.Appointment;
//import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
//import com.example.doctor_patient_management_system.repository.AppointmentRepository;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.stream.Collectors;
//
//
//@Configuration
//public class StartupConfig {
//
//    private static final Logger logger = LoggerFactory.getLogger(StartupConfig.class);
//
//    @Bean
//    @Transactional
//    public CommandLineRunner autoCancelPastAppointmentsOnStartup(
//            AppointmentRepository appointmentRepository) {
//
//        return args -> {
//            try {
//                LocalDate today = LocalDate.now();
//
//                logger.info("===========================================");
//                logger.info("🚀 Application Startup: Checking for past appointments...");
//                logger.info("Current Date: {}", today);
//
//                // Past date এর সব CONFIRMED appointments খুঁজে বের করুন
//                List<Appointment> pastAppointments = appointmentRepository
//                        .findAll()
//                        .stream()
//                        .filter(a -> a.getAppointmentDate() != null)
//                        .filter(a -> a.getAppointmentDate().isBefore(today))
//                        .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
//                        .collect(Collectors.toList());
//
//                if (pastAppointments.isEmpty()) {
//                    logger.info("✅ No past appointments found to cancel");
//                    logger.info("===========================================");
//                    return;
//                }
//
//                logger.info("📋 Found {} past appointments to cancel:", pastAppointments.size());
//
//                // Status update করুন এবং details log করুন
//                for (Appointment appointment : pastAppointments) {
//                    logger.info("   - Appointment ID: {}, Date: {}, Doctor ID: {}, Patient ID: {}",
//                            appointment.getId(),
//                            appointment.getAppointmentDate(),
//                            appointment.getDoctor().getId(),
//                            appointment.getPatient().getId());
//
//                    appointment.setStatus(AppointmentStatus.CANCELLED);
//                }
//
//                // Batch save - efficient
//                appointmentRepository.saveAll(pastAppointments);
//
//                logger.info("✅ Successfully cancelled {} past appointments", pastAppointments.size());
//                logger.info("===========================================");
//
//            } catch (Exception e) {
//                logger.error("❌ Error while cancelling past appointments on startup: {}",
//                        e.getMessage(), e);
//                logger.error("===========================================");
//            }
//        };
//    }
//}