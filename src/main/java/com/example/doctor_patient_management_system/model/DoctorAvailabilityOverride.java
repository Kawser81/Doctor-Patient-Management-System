package com.example.doctor_patient_management_system.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "doctor_availability_overrides")
public class DoctorAvailabilityOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDate overrideDate;

    @Column
    private Boolean isAvailable;

    public DoctorAvailabilityOverride() {}

    public DoctorAvailabilityOverride(Doctor doctor, LocalDate overrideDate, Boolean isAvailable) {
        this.doctor = doctor;
        this.overrideDate = overrideDate;
        this.isAvailable = isAvailable;
    }

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

    public LocalDate getOverrideDate() {
        return overrideDate;
    }

    public void setOverrideDate(LocalDate overrideDate) {
        this.overrideDate = overrideDate;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
}



//
//@Entity
//@Table(name = "doctor_availability_overrides")
//public class DoctorAvailabilityOverride {
//
//    @Id
//    @GeneratedValue
//
//    private Long id;
//    @ManyToOne
//
//    private Doctor doctor;
//    private LocalDate overrideDate;
//    private Boolean isAvailable;
//    public DoctorAvailabilityOverride() {}
//
//    public DoctorAvailabilityOverride(Long id, Doctor doctor, LocalDate overrideDate, Boolean isAvailable) {
//        this.id = id;
//        this.doctor = doctor;
//        this.overrideDate = overrideDate;
//        this.isAvailable = isAvailable;
//    }
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public Doctor getDoctor() {
//        return doctor;
//    }
//
//    public void setDoctor(Doctor doctor) {
//        this.doctor = doctor;
//    }
//
//    public LocalDate getOverrideDate() {
//        return overrideDate;
//    }
//
//    public void setOverrideDate(LocalDate overrideDate) {
//        this.overrideDate = overrideDate;
//    }
//
//    public Boolean getAvailable() {
//        return isAvailable;
//    }
//
//    public void setAvailable(Boolean available) {
//        isAvailable = available;
//    }
//}