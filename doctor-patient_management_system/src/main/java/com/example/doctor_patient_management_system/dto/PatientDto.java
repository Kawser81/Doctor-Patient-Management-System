package com.example.doctor_patient_management_system.dto;

import jakarta.validation.constraints.NotBlank;

public class PatientDto {

    @NotBlank
    private String patientName;

    @NotBlank
    private String patientEmail;

    @NotBlank
    private String gender;

    @NotBlank
    private String contact;

    public PatientDto() {}

    public PatientDto(String patientName, String patientEmail, String gender, String contact) {
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.gender = gender;
        this.contact = contact;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientEmail() {
        return patientEmail;
    }

    public void setPatientEmail(String patientEmail) {
        this.patientEmail = patientEmail;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
}
