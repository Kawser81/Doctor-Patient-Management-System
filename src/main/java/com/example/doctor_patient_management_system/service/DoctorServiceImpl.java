package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.*;
//import com.example.doctor_patient_management_system.dto.SlotDto;
import com.example.doctor_patient_management_system.model.*;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import com.example.doctor_patient_management_system.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final DoctorAvailabilityOverrideRepository doctorAvailabilityOverrideRepository;
    private final ReviewRepository reviewRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository,
                             UserRepository userRepository,
                             DoctorAvailabilityOverrideRepository doctorAvailabilityOverrideRepository,
                             ReviewRepository reviewRepository) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.doctorAvailabilityOverrideRepository = doctorAvailabilityOverrideRepository;
        this.reviewRepository = reviewRepository;
    }


    @Override
    @Cacheable(value = "doctors", key = "'all'")
    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }


    @Override
    public List<Doctor> findBySpeciality(String speciality) {
        return doctorRepository.findBySpecialityIgnoreCase(speciality);
    }

    @Override
    public List<String> getAllSpecialitiesSorted() {
        return doctorRepository.findDistinctSpecialityOrderBySpecialityAsc();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "doctors", key = "'all'"),
            @CacheEvict(value = "doctorsWithRatings", allEntries = true),
            @CacheEvict(value = "doctor", key = "#userId")
    })
    public Doctor createDoctorProfile(Long userId, DoctorDto dto) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.isComplete()) {
                throw new RuntimeException("Profile already completed");
            }

            Doctor doctor = new Doctor();
            doctor.setUser(user);
            doctor.setId(userId);
            doctor.setDoctorName(dto.getDoctorName());
            doctor.setDegree(dto.getDegree());
            doctor.setSpeciality(dto.getSpeciality());
            doctor.setEmail(dto.getEmail() != null ? dto.getEmail() : user.getEmail());
            doctor.setAddress(dto.getAddress());
            doctor.setContact(dto.getContact());
            doctor.setConsultationStartTime(dto.getConsultationStartTime());
            doctor.setConsultationEndTime(dto.getConsultationEndTime());
            doctor.setOffDays(dto.getOffDays());
            doctor.setConsultationFee(dto.getConsultationFee());

            Doctor savedDoctor = doctorRepository.save(doctor);

            user.setComplete(true);
            userRepository.save(user);

            return savedDoctor;

        } catch (RuntimeException e) {
            System.out.println("Exception: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "doctors", key = "'all'"),
            @CacheEvict(value = "doctorsWithRatings", allEntries = true),
            @CacheEvict(value = "doctor", key = "#userId")
    })
    public Doctor updateDoctorProfile(Long userId, DoctorDto dto) {
        Doctor doctor = doctorRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        doctor.setDoctorName(dto.getDoctorName());
        doctor.setEmail(dto.getEmail());
        doctor.setDegree(dto.getDegree());
        doctor.setSpeciality(dto.getSpeciality());
        doctor.setConsultationStartTime(dto.getConsultationStartTime());
        doctor.setConsultationEndTime(dto.getConsultationEndTime());
        doctor.setOffDays(dto.getOffDays());
        doctor.setConsultationFee(dto.getConsultationFee());
        doctor.setAddress(dto.getAddress());
        doctor.setContact(dto.getContact());

        Doctor updatedDoctor = doctorRepository.save(doctor);

        return updatedDoctor;

    }

    @Override
    public DoctorDto getDoctorEditDto(Long id, String email) {

        Doctor doctor = doctorRepository.findById(id).orElseThrow();
        Optional<User> currentUserOpt = userRepository.findByEmail(email);

        boolean canEdit = currentUserOpt.isPresent() &&
                (currentUserOpt.get().getId().equals(id) || currentUserOpt.get().getRole() == Role.ADMIN);

        if (!canEdit) {
            throw new AccessDeniedException("Cannot edit this profile");
        }

        DoctorDto dto = new DoctorDto();
        dto.setDoctorName(doctor.getDoctorName());
        dto.setEmail(doctor.getEmail());
        dto.setDegree(doctor.getDegree());
        dto.setSpeciality(doctor.getSpeciality());
        dto.setAddress(doctor.getAddress());
        dto.setContact(doctor.getContact());
        dto.setConsultationStartTime(doctor.getConsultationStartTime());
        dto.setConsultationEndTime(doctor.getConsultationEndTime());
        dto.setOffDays(doctor.getOffDays());
        dto.setConsultationFee(doctor.getConsultationFee());

        return dto;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "doctors", key = "'all'"),
            @CacheEvict(value = "doctorsWithRatings", allEntries = true),
            @CacheEvict(value = "doctor", key = "#doctorId")
    })
    public void deleteDoctor(Long doctorId) {
        doctorRepository.deleteById(doctorId);
    }

    @Override
    public Doctor getDoctorById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + id));
    }

    @Override
    @Cacheable(value = "doctor", key = "#id")
    public Doctor findById(Long id) {
        return doctorRepository.findById(id).orElseThrow(() -> new RuntimeException("Doctor not found"));
    }

    @Override
    public List<DoctorAvailabilityOverride> getUpcomingBlocks(Long doctorId) {
        return doctorAvailabilityOverrideRepository.findUpcomingByDoctorId(doctorId, LocalDate.now());
    }

    @Override
    @Transactional
    public void blockDay(Long doctorId, LocalDate date) {
        Optional<DoctorAvailabilityOverride> existing = doctorAvailabilityOverrideRepository.findByDoctorIdAndDate(doctorId, date);
        DoctorAvailabilityOverride override = existing.orElse(new DoctorAvailabilityOverride());
        override.setDoctor(doctorRepository.findById(doctorId).orElseThrow());
        override.setOverrideDate(date);
        override.setIsAvailable(false);
        doctorAvailabilityOverrideRepository.save(override);
    }

    @Override
    @Transactional
    public void unblockDay(Long doctorId, LocalDate date) {
        Optional<DoctorAvailabilityOverride> existing = doctorAvailabilityOverrideRepository.findByDoctorIdAndDate(doctorId, date);
        if (existing.isPresent()) {
            doctorAvailabilityOverrideRepository.delete(existing.get());
        }
    }
    @Override
    public Double getAverageRatingForDoctor(Long doctorId) {
        return reviewRepository.findAverageRatingByDoctorId(doctorId);
    }

    @Override
    public Long getReviewCountForDoctor(Long doctorId) {
        return reviewRepository.countReviewsByDoctorId(doctorId);
    }

    @Override
    public Page<ReviewDto> getReviewsForDoctorPaginated(Long doctorId, Pageable pageable) {
        return reviewRepository.findReviewsWithPatientByDoctorId(doctorId, pageable);
    }

    @Override
    public List<ReviewDto> getReviewsForDoctor(Long doctorId) {
        return reviewRepository.findReviewsWithPatientByDoctorId(doctorId, Pageable.unpaged()).getContent();
    }

    @Override
    @Cacheable(value = "doctorsWithRatings", key = "'all'")
    public List<DoctorWithRatingDto> findAllWithAverageRatings() {
        return doctorRepository.findAllWithAverageRatings();
    }
}
