package com.example.doctor_patient_management_system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDate;

public class BookingMessage implements Serializable {

    //For RabbitMQ needed Serializable

    private static final long serialVersionUID = 1L;

    private Long appointmentId;
    private String patientEmail;
    private String patientName;
    private String doctorEmail;
    private String doctorName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    private String appointmentTime;
    private String speciality;
    private Integer consultationFee;


    public BookingMessage() {}


    public BookingMessage(Long appointmentId, String patientEmail, String patientName,
                          String doctorEmail, String doctorName, LocalDate appointmentDate,
                          String appointmentTime, String speciality, Integer consultationFee) {
        this.appointmentId = appointmentId;
        this.patientEmail = patientEmail;
        this.patientName = patientName;
        this.doctorEmail = doctorEmail;
        this.doctorName = doctorName;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.speciality = speciality;
        this.consultationFee = consultationFee;
    }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorEmail() { return doctorEmail; }
    public void setDoctorEmail(String doctorEmail) { this.doctorEmail = doctorEmail; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }

    public String getSpeciality() { return speciality; }
    public void setSpeciality(String speciality) { this.speciality = speciality; }

    public Integer getConsultationFee() { return consultationFee; }
    public void setConsultationFee(Integer consultationFee) { this.consultationFee = consultationFee; }

    @Override
    public String toString() {
        return "BookingMessage{" +
                "appointmentId=" + appointmentId +
                ", patientEmail='" + patientEmail + '\'' +
                ", doctorName='" + doctorName + '\'' +
                ", appointmentDate=" + appointmentDate +
                ", appointmentTime='" + appointmentTime + '\'' +
                '}';
    }

    //Why use toString?
    //Convert Object into human readable string

}