package com.example.doctor_patient_management_system.repository;

import com.example.doctor_patient_management_system.model.Doctor;
import com.example.doctor_patient_management_system.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Integer> {
    Optional<Patient> findByPatientEmail(String patientEmail);
    Patient deleteById(Long id);
    Optional<Patient> findById(Long id);
    Optional<Patient> findByUserId(Long userId);
}
