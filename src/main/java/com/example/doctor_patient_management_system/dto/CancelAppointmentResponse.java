package com.example.doctor_patient_management_system.dto;

public class CancelAppointmentResponse {
    private boolean success;
    private String error;

    public CancelAppointmentResponse(boolean success) {
        this.success = success;
    }

    public CancelAppointmentResponse(String error) {
        this.success = false;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}