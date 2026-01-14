package com.example.doctor_patient_management_system.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name="patients_info")
public class Patient {

    @Id
    private Long id;
    private String patientName;
    private String patientEmail;
    private String gender;
    @NotBlank(message = "Contact number is required")
    @Pattern(
            regexp = "^01[0-9]{9}$",
            message = "Contact number must be 11 digits starting with 01 (e.g., 01712345678)"
    )
    private String contact;

    @OneToOne
    @MapsId
    @JoinColumn(name="id")
    @JsonIgnore
    private User user;

    public Patient() {}

    public Patient(Long id, String patientName, String patientEmail, String gender, String contact) {
        this.id = id;
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.gender = gender;
        this.contact = contact;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
