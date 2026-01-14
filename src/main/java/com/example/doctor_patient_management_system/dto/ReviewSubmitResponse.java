package com.example.doctor_patient_management_system.dto;

public class ReviewSubmitResponse {
    private boolean success;
    private Long reviewId;
    private String error;

    public ReviewSubmitResponse(boolean success, Long reviewId, String error) {
        this.success = success;
        this.reviewId = reviewId;
        this.error = error;
    }

    public boolean isSuccess() { return success; }
    public Long getReviewId() { return reviewId; }
    public String getError() { return error; }
}