package com.example.doctor_patient_management_system.model; // Adjust to your exact package

import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    @JsonIgnore
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient; // NEW: Bidirectional mapping to User (replaces plain Long patientId)

    @Column(name = "slot_id")
    private Integer slotId;

    @Column(name = "appointment_date")
    private LocalDate appointmentDate;

    @Column(name = "appointment_time")
    private String appointmentTime;

//    @Column(name = "status")
//    private String status = "CONFIRMED";

    @Enumerated(EnumType.STRING)  // ADD THIS
    @Column(name = "status")
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    @OneToOne(mappedBy = "appointment", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Prescription prescription;

    // Constructors
    public Appointment() {}

    public Appointment(Doctor doctor, User patient, Integer slotId, LocalDate appointmentDate, String appointmentTime) {
        this.doctor = doctor;
        this.patient = patient; // Updated to use User object
        this.slotId = slotId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public User getPatient() {
        return patient;
    }

    public void setPatient(User patient) {
        this.patient = patient;
    }

    public Integer getSlotId() {
        return slotId;
    }

    public void setSlotId(Integer slotId) {
        this.slotId = slotId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public Prescription getPrescription() {
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }


//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//
//    public Doctor getDoctor() { return doctor; }
//    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
//
//    public User getPatient() { return patient; } // NEW: Getter for the association
//    public void setPatient(User patient) { this.patient = patient; }
//
//    // Legacy support: If you need patientId as Long (e.g., for APIs), add this transient method
//    @Transient
//    public Long getPatientId() {
//        return patient != null ? patient.getId() : null;
//    }
//
//    public Integer getSlotId() { return slotId; }
//    public void setSlotId(Integer slotId) { this.slotId = slotId; }
//
//    public LocalDate getAppointmentDate() { return appointmentDate; }
//    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
//
//    public String getAppointmentTime() { return appointmentTime; }
//    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }
//
//    public String getStatus() { return status; }
//    public void setStatus(String status) { this.status = status; }
//
//    public Prescription getPrescription() {
//        return prescription;
//    }
//
//    public void setPrescription(Prescription prescription) {
//        this.prescription = prescription;
//    }
}















//package com.example.doctor_patient_management_system.model;
//
//import jakarta.persistence.*;
//import java.time.LocalDate;
//
//@Entity
//@Table(name = "appointments")
//public class Appointment {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne
//    @JoinColumn(name = "doctor_id", nullable = false)
//    private Doctor doctor;
//
//    @Column(name = "patient_id", nullable = false)
//    private Long patientId; // Foreign key to users.id (for patient)
//
//    @Column(name = "slot_id")
//    private Integer slotId;
//
//    @Column(name = "appointment_date")
//    private LocalDate appointmentDate;
//
//    @Column(name = "appointment_time")
//    private String appointmentTime; // e.g., "09:00 AM - 09:20 AM"
//
//    @Column(name = "status")
//    private String status = "CONFIRMED";
//
//    // Constructors
//    public Appointment() {}
//
//    public Appointment(Doctor doctor, Long patientId, Integer slotId, LocalDate appointmentDate, String appointmentTime) {
//        this.doctor = doctor;
//        this.patientId = patientId;
//        this.slotId = slotId;
//        this.appointmentDate = appointmentDate;
//        this.appointmentTime = appointmentTime;
//    }
//
//    // Getters and Setters
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//
//    public Doctor getDoctor() { return doctor; }
//    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
//
//    public Long getPatientId() { return patientId; }
//    public void setPatientId(Long patientId) { this.patientId = patientId; }
//
//    public Integer getSlotId() { return slotId; }
//    public void setSlotId(Integer slotId) { this.slotId = slotId; }
//
//    public LocalDate getAppointmentDate() { return appointmentDate; }
//    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
//
//    public String getAppointmentTime() { return appointmentTime; }
//    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }
//
//    public String getStatus() { return status; }
//    public void setStatus(String status) { this.status = status; }
//}