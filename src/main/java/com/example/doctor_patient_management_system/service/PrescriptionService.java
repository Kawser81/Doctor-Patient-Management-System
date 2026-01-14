package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.Prescription;
import jakarta.validation.Valid;

import java.util.Optional;

public interface PrescriptionService {

     Optional<Prescription> getByAppointmentId(Long appointmentId);

     void savePrescription(@Valid Prescription prescription, Appointment appointment);

}
