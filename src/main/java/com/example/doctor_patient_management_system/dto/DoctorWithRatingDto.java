package com.example.doctor_patient_management_system.dto;

import com.example.doctor_patient_management_system.model.Doctor;

public class DoctorWithRatingDto {
    private Doctor doctor;
    private Double averageRating;
    private Long reviewCount;

    public DoctorWithRatingDto() {}

    public DoctorWithRatingDto(Doctor doctor, Double averageRating, Long reviewCount) {
        this.doctor = doctor;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount != null ? reviewCount : 0L;
    }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Long getReviewCount() { return reviewCount; }
    public void setReviewCount(Long reviewCount) { this.reviewCount = reviewCount; }
}