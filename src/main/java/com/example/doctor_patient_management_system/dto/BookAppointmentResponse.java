
package com.example.doctor_patient_management_system.dto;

public class BookAppointmentResponse {
    private boolean success;
    private String message;
    private Long appointmentId;
    private String error;

    // Success constructor
    public BookAppointmentResponse(boolean success, String message, Long appointmentId) {
        this.success = success;
        this.message = message;
        this.appointmentId = appointmentId;
    }

    // Error constructor
    public BookAppointmentResponse(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}