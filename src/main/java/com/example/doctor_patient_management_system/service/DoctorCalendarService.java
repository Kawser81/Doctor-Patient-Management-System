package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.DayStatus;

import java.util.List;

public interface DoctorCalendarService {

    List<DayStatus> getCalendarData(Long doctorId, int year, int month);

}
