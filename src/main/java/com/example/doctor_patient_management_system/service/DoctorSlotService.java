package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.TimeSlot;

import java.time.LocalDate;
import java.util.List;

public interface DoctorSlotService {

    List<TimeSlot> getAvailableSlots(Long doctorId, LocalDate date);

}
