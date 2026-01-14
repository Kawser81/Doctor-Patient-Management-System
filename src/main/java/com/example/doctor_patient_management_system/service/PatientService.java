package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.PatientDto;
import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.Patient;
import com.example.doctor_patient_management_system.model.Review;
import com.example.doctor_patient_management_system.model.User;

import java.time.LocalDate;
import java.util.Optional;

public interface PatientService {

     Optional<Patient> getPatientById(Long id);

     Patient updatePatientProfile(Long userId, PatientDto dto);

     Patient findPatientByUserIdOrNull(Long userId);

     Patient createPatientProfile(Long userId, PatientDto dto, User user);

     Optional<Patient> getDoctorByUserId(Long userId);

     Optional<Patient> getPatientByUserId(Long userId);

     Appointment bookAppointment(Long doctorId, Long patientId, LocalDate appointmentDate, String appointmentTime);

     Review submitReview(Long appointmentId, Long patientId, Integer rating, String comment);

     Optional<Review> getReviewByAppointmentId(Long appointmentId);

     Patient gotPatientById(Long id);

     Patient getByUserId(Long userId);

}
