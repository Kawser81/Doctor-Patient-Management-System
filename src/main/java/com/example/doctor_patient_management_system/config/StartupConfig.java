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

    @Bean
    @Transactional
    public CommandLineRunner autoCancelPastAppointmentsOnStartup(
            AppointmentRepository appointmentRepository) {

        return args -> {
            try {
                LocalDate today = LocalDate.now();

                List<Appointment> pastAppointments = appointmentRepository
                        .findAll()
                        .stream()
                        .filter(a -> a.getAppointmentDate() != null)
                        .filter(a -> a.getAppointmentDate().isBefore(today))
                        .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                        .filter(a -> a.getPrescription() == null)
                        .collect(Collectors.toList());

                if (pastAppointments.isEmpty()) {
                    return;
                }

                for (Appointment appointment : pastAppointments) {
                    appointment.setStatus(AppointmentStatus.CANCELLED);
                }

                appointmentRepository.saveAll(pastAppointments);

            } catch (Exception e) {
            }
        };
    }

    //After starting the application remove the past appointment automatically
}