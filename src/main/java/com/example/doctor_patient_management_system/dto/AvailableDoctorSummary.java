package com.example.doctor_patient_management_system.dto;

import com.example.doctor_patient_management_system.model.Doctor;
import java.time.LocalDate;

public class AvailableDoctorSummary {
    private Doctor doctor;
    private LocalDate nextAvailableDate;
    private int availableSlotsNextWeek;

    public AvailableDoctorSummary(Doctor doctor, LocalDate nextAvailableDate, int availableSlotsNextWeek) {
        this.doctor = doctor;
        this.nextAvailableDate = nextAvailableDate;
        this.availableSlotsNextWeek = availableSlotsNextWeek;
    }

    public AvailableDoctorSummary(Doctor doctor, LocalDate nextAvailableDate) {
        this.doctor = doctor;
        this.nextAvailableDate = nextAvailableDate;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public LocalDate getNextAvailableDate() {
        return nextAvailableDate;
    }

    public int getAvailableSlotsNextWeek() {
        return availableSlotsNextWeek;
    }

}