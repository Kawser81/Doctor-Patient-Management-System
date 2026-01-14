package com.example.doctor_patient_management_system.repository;

import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByDoctorIdAndAppointmentDateBetween(Long doctorId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT a.slotId FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate = :date AND a.status = 'CONFIRMED'")
    List<Integer> findBookedSlotIdsByDoctorIdAndDate(@Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId ORDER BY a.appointmentDate DESC")
    List<Appointment> findByPatientIdOrderByAppointmentDateDesc(@Param("patientId") Long patientId);

    List<Appointment> findByDoctorIdOrderByAppointmentDateDesc(Long doctorId);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate = :date AND a.appointmentTime = :time AND a.status != :cancelledStatus")
    List<Appointment> findByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            @Param("doctorId") Long doctorId, @Param("date") LocalDate date, @Param("time") String time,
            @Param("cancelledStatus") AppointmentStatus cancelledStatus);

    void deleteByDoctorId(Long doctorId);
    void deleteByPatientId(Long patientId);

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTime(
            Long doctorId,
            LocalDate appointmentDate,
            String appointmentTime);

    List<Appointment> findByDoctorIdAndAppointmentDateAndStatus(
            Long doctorId,
            LocalDate appointmentDate,
            AppointmentStatus status
    );

}
