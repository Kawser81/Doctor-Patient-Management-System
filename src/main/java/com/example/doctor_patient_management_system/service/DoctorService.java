package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.DoctorDto;
import com.example.doctor_patient_management_system.dto.DoctorWithRatingDto;
import com.example.doctor_patient_management_system.dto.ReviewDto;
import com.example.doctor_patient_management_system.model.Doctor;
import com.example.doctor_patient_management_system.model.DoctorAvailabilityOverride;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface DoctorService {

     List<Doctor> findAll();

     List<Doctor> findBySpeciality(String speciality);

     List<String> getAllSpecialitiesSorted();

     Doctor createDoctorProfile(Long userId, DoctorDto dto);

     Doctor updateDoctorProfile(Long userId, DoctorDto dto);

     DoctorDto getDoctorEditDto(Long id, String email);

     void deleteDoctor(Long doctorId);

     Doctor getDoctorById(Long id);

     Doctor findById(Long id);

     List<DoctorAvailabilityOverride> getUpcomingBlocks(Long doctorId);

     void blockDay(Long doctorId, LocalDate date);

     void unblockDay(Long doctorId, LocalDate date);

     Double getAverageRatingForDoctor(Long doctorId);

     Long getReviewCountForDoctor(Long doctorId);

     Page<ReviewDto> getReviewsForDoctorPaginated(Long doctorId, Pageable pageable);

     List<ReviewDto> getReviewsForDoctor(Long doctorId);

     List<DoctorWithRatingDto> findAllWithAverageRatings();

}
