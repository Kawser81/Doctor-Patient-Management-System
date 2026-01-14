package com.example.doctor_patient_management_system.repository;

import com.example.doctor_patient_management_system.dto.DoctorWithRatingDto;
import com.example.doctor_patient_management_system.model.Doctor;
import com.example.doctor_patient_management_system.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByEmail(String email);
    @Query("SELECT d FROM Doctor d JOIN FETCH d.user WHERE d.user.email = :email")
    Optional<Doctor> findByUserEmail(@Param("email") String email);
    Optional<Doctor> findById(Long id);
    Optional<Doctor> findByUserId(Long userId);

    List<Doctor> findBySpecialityIgnoreCase(String speciality);

    @Query("SELECT DISTINCT d.speciality FROM Doctor d WHERE d.speciality IS NOT NULL ORDER BY d.speciality")
    List<String> findDistinctSpecialityOrderBySpecialityAsc();

    //
    @Query("SELECT new com.example.doctor_patient_management_system.dto.DoctorWithRatingDto(" +
            "d, AVG(r.rating), COUNT(r)) " +
            "FROM Doctor d " +
            "LEFT JOIN Appointment a ON d.id = a.doctor.id " +  // Join to appointments
            "LEFT JOIN Review r ON a.id = r.appointment.id " +   // Join to reviews
            "GROUP BY d.id " +                                   // Group by doctor
            "ORDER BY d.doctorName ASC")                         // Optional: Sort by name
    List<DoctorWithRatingDto> findAllWithAverageRatings();

}
