package com.example.doctor_patient_management_system.repository;

import com.example.doctor_patient_management_system.dto.ReviewDto;
import com.example.doctor_patient_management_system.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByAppointmentId(Long appointmentId);
    boolean existsByAppointmentId(Long appointmentId);

    List<Review> findByAppointmentDoctorIdOrderByCreatedAtDesc(Long doctorId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.appointment.doctor.id = :doctorId")
    Double findAverageRatingByDoctorId(@Param("doctorId") Long doctorId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.appointment.doctor.id = :doctorId")
    Long countReviewsByDoctorId(@Param("doctorId") Long doctorId);


    //For finding patient_id
    @Query("SELECT new com.example.doctor_patient_management_system.dto.ReviewDto(" +
            "r.rating, r.comment, r.createdAt, p.patientName) " +
            "FROM Review r " +
            "JOIN r.appointment a " +
            "JOIN Patient p ON a.patient.id = p.id " +
            "WHERE a.doctor.id = :doctorId " +
            "ORDER BY r.createdAt DESC")
    Page<ReviewDto> findReviewsWithPatientByDoctorId(@Param("doctorId") Long doctorId, Pageable pageable);

}