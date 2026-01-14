package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.repository.AppointmentRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public List<Appointment> getPatientIdByAppointment(Long id) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(id);
    }

    @Override
    public Appointment getById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found with ID: " + appointmentId));
    }

    @Override
    public void cancelAppointment(Appointment appointment) {
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }


    @Override
    public List<Appointment> findByDoctorIdOrderByAppointmentDateDesc(Long doctorId) {
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctorId);
    }

    @Override
    public void cancel(Appointment appointment) {
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public int cancelConfirmedAppointmentsForDoctorOnDate(Long doctorId, LocalDate date) {
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateAndStatus(doctorId, date, AppointmentStatus.CONFIRMED);

        int count = appointments.size();
        for (Appointment appointment : appointments) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointmentRepository.save(appointment);
        }

        return count;
    }

    @Override
    public Appointment bookAppointment(Appointment appointment) {
        List<Integer> bookedSlots = appointmentRepository.findBookedSlotIdsByDoctorIdAndDate(
                appointment.getDoctor().getId(), appointment.getAppointmentDate());

        if (bookedSlots.contains(appointment.getSlotId())) {
            throw new RuntimeException("Slot already booked");
        }
        return appointmentRepository.save(appointment);
    }

}
