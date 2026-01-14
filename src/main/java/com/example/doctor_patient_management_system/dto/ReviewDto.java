package com.example.doctor_patient_management_system.dto;

import java.time.LocalDateTime;

public class ReviewDto {
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private String patientName;

    public ReviewDto() {}

    public ReviewDto(Integer rating, String comment, LocalDateTime createdAt, String patientName) {
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.patientName = patientName;
    }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
}