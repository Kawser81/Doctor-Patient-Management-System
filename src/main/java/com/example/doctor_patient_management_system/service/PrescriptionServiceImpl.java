package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.Prescription;
import com.example.doctor_patient_management_system.repository.PrescriptionRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    @Override
    public Optional<Prescription> getByAppointmentId(Long appointmentId) {
        return prescriptionRepository.findByAppointmentId(appointmentId);
    }

    @Override
    public void savePrescription(@Valid Prescription prescription, Appointment appointment) {
        prescription.setAppointment(appointment);
        prescription.setCreatedAt(LocalDateTime.now());
        prescriptionRepository.save(prescription);
    }
}
