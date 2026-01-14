package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.AvailableDoctorSummary;
import com.example.doctor_patient_management_system.model.DoctorAvailability;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DoctorAvailabilityService {

     Map<DayOfWeek, List<DoctorAvailability>> getAvailabilityByDay(Long doctorId);

     void clearAvailability(Long doctorId);

     DoctorAvailability save(DoctorAvailability availability);

     List<AvailableDoctorSummary> getAvailableDoctors(int limit);

     boolean isOffDay(LocalDate date, Long doctorId);

     int totalSlotsPerDay(Long doctorId);

     List<AvailableDoctorSummary> getUpcomingAvailableFlatList(int limit, String specialityFilter);

}
