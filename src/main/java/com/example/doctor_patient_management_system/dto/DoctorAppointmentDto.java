package com.example.doctor_patient_management_system.dto;


import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.Patient;
import com.example.doctor_patient_management_system.model.Prescription;

public class DoctorAppointmentDto {
    private Appointment appointment;
    private Patient patientProfile;
    private Prescription prescription;

    public DoctorAppointmentDto() {}

    public DoctorAppointmentDto(Appointment appointment, Patient patientProfile, Prescription prescription) {
        this.appointment = appointment;
        this.patientProfile = patientProfile;
        this.prescription = prescription;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public Patient getPatientProfile() {
        return patientProfile;
    }

    public void setPatientProfile(Patient patientProfile) {
        this.patientProfile = patientProfile;
    }

    public Prescription getPrescription() {
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }
}