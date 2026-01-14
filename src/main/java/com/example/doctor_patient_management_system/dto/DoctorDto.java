package com.example.doctor_patient_management_system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class DoctorDto {

    @NotBlank(message = "Doctor name is required")
    private String doctorName;

    private String email;

    @NotBlank(message = "Degree is required")
    private String degree;

    @NotBlank(message = "Speciality is required")
    private String speciality;

    @NotBlank(message = "Start time is required")
    private String consultationStartTime;  // e.g., "09:00"

    @NotBlank(message = "End time is required")
    private String consultationEndTime;    // e.g., "18:00"

    private String offDays;  // e.g., "FRIDAY,SATURDAY" (optional)

    @NotNull(message = "Consultation fee is required")
    @Min(value = 100, message = "Fee must be at least 100 BDT")
    private Integer consultationFee;

    @NotBlank(message = "Clinic address is required")
    private String address;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^(\\+880|0)(1[3-9]|2)[0-9]{8}$",
            message = "Invalid Bangladeshi mobile number")
    private String contact;

    public DoctorDto() {}

    public DoctorDto(String doctorName, String email, String degree, String speciality, String consultationStartTime, String consultationEndTime, String offDays, Integer consultationFee, String address, String contact) {
        this.doctorName = doctorName;
        this.email = email;
        this.degree = degree;
        this.speciality = speciality;
        this.consultationStartTime = consultationStartTime;
        this.consultationEndTime = consultationEndTime;
        this.offDays = offDays;
        this.consultationFee = consultationFee;
        this.address = address;
        this.contact = contact;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getSpeciality() {
        return speciality;
    }

    public void setSpeciality(String speciality) {
        this.speciality = speciality;
    }

    public String getConsultationStartTime() {
        return consultationStartTime;
    }

    public void setConsultationStartTime(String consultationStartTime) {
        this.consultationStartTime = consultationStartTime;
    }

    public String getConsultationEndTime() {
        return consultationEndTime;
    }

    public void setConsultationEndTime(String consultationEndTime) {
        this.consultationEndTime = consultationEndTime;
    }

    public String getOffDays() {
        return offDays;
    }

    public void setOffDays(String offDays) {
        this.offDays = offDays;
    }

    public Integer getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(Integer consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
}