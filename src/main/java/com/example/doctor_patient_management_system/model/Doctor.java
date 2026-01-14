package com.example.doctor_patient_management_system.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

@Entity
@Table(name = "doctors")
public class Doctor {
    @Id
    private Long id;

    @Column(nullable = false)
    private String doctorName;

    @Column(nullable = false)
    private String speciality;

    private String email;

    private String degree;

    private String consultationStartTime;

    private String consultationEndTime;

    private String address;

    @NotBlank(message = "Contact number is required")
    @Pattern(
            regexp = "^01[0-9]{9}$",
            message = "Contact number must be 11 digits starting with 01 (e.g., 01712345678)"
    )
    private String contact;

    @Version
    private Long version;

    @Column(name = "consultation_fee")
    private Integer consultationFee;

    @Column(name = "off_days")
    private String offDays;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    @JsonIgnore
    private User user;


//    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL)
//    private List<Appointment> appointments;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  // Don't serialize this to prevent circular references
    private List<Appointment> appointments;

    public Doctor() {}

    public Doctor(User user, String doctorName, String speciality, String email, String degree,
                  String consultationStartTime, String consultationEndTime, String address,
                  String contact, Integer consultationFee, String offDays) {
        this.user = user;
        this.doctorName = doctorName;
        this.speciality = speciality;
        this.email = email;
        this.degree = degree;
        this.consultationStartTime = consultationStartTime;
        this.consultationEndTime = consultationEndTime;
        this.address = address;
        this.contact = contact;
        this.consultationFee = consultationFee;
        this.offDays = offDays;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.id = user.getId();
        }
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getSpeciality() {
        return speciality;
    }

    public void setSpeciality(String speciality) {
        this.speciality = speciality;
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

    public Integer getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(Integer consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getOffDays() {
        return offDays;
    }

    public void setOffDays(String offDays) {
        this.offDays = offDays;
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
