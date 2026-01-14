package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.model.Appointment;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

     List<Appointment> getPatientIdByAppointment(Long id);

     Appointment getById(Long appointmentId);

     void cancelAppointment(Appointment appointment);

     List<Appointment> findByDoctorIdOrderByAppointmentDateDesc(Long doctorId);

     void cancel(Appointment appointment);

     int cancelConfirmedAppointmentsForDoctorOnDate(Long doctorId, LocalDate date);

     Appointment bookAppointment(Appointment appointment);

}
