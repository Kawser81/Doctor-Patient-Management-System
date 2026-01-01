package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.model.DoctorAvailability;
import com.example.doctor_patient_management_system.repository.DoctorAvailabilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DoctorAvailabilityService {

    @Autowired
    private DoctorAvailabilityRepository availabilityRepository;

    public Map<DayOfWeek, List<DoctorAvailability>> getAvailabilityByDay(Long doctorId) {
        List<DoctorAvailability> availabilities =
                availabilityRepository.findByDoctorIdAndIsAvailableTrue(doctorId);

        return availabilities.stream()
                .sorted(Comparator.comparing(DoctorAvailability::getStartTime))
                .collect(Collectors.groupingBy(DoctorAvailability::getDayOfWeek));
    }

    public void clearAvailability(Long doctorId) {
        List<DoctorAvailability> existing =
                availabilityRepository.findByDoctorId(doctorId);
        availabilityRepository.deleteAll(existing);
    }

    public DoctorAvailability save(DoctorAvailability availability) {
        return availabilityRepository.save(availability);
    }
}