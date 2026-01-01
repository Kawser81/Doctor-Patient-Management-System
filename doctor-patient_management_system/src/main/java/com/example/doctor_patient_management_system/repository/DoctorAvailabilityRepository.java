package com.example.doctor_patient_management_system.repository;

import com.example.doctor_patient_management_system.model.Doctor;
import com.example.doctor_patient_management_system.model.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {
    List<DoctorAvailability> findByDoctorIdAndIsAvailableTrue(Long doctorId);
    List<DoctorAvailability> findByDoctorAndDayOfWeek(Doctor doctor, DayOfWeek dayOfWeek);
    List<DoctorAvailability> findByDoctorId(Long doctorId);
}
