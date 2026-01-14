package com.example.doctor_patient_management_system.dto;

import java.time.LocalDate;

public class BookAppointmentRequest {
    private int slotId;
    private LocalDate appointmentDate;

    public BookAppointmentRequest() {
    }

    public BookAppointmentRequest(int slotId, LocalDate appointmentDate) {
        this.slotId = slotId;
        this.appointmentDate = appointmentDate;
    }

    public int getSlotId() {
        return slotId;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }
}