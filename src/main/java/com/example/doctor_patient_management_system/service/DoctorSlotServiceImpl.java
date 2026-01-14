package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.TimeSlot;
import com.example.doctor_patient_management_system.model.Doctor;
import com.example.doctor_patient_management_system.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DoctorSlotServiceImpl implements DoctorSlotService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorServiceImpl doctorService;

    public DoctorSlotServiceImpl(AppointmentRepository appointmentRepository,
                                 DoctorServiceImpl doctorService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorService = doctorService;
    }

    @Override
    public List<TimeSlot> getAvailableSlots(Long doctorId, LocalDate date) {

        List<Integer> booked = appointmentRepository.findBookedSlotIdsByDoctorIdAndDate(doctorId, date);

        List<TimeSlot> allSlots = generateAllTimeSlots(doctorId);

        // Filter out booked slots
        List<TimeSlot> availableSlots = allSlots.stream()
                .filter(slot -> !booked.contains(slot.getId()))
                .collect(Collectors.toList());

        return availableSlots;
    }

    private List<TimeSlot> generateAllTimeSlots(Long doctorId) {
        Doctor doctor = doctorService.getDoctorById(doctorId);
        if (doctor == null) {
            return new ArrayList<>();
        }

        String startTimeStr = doctor.getConsultationStartTime();
        String endTimeStr = doctor.getConsultationEndTime();

        List<TimeSlot> slots = new ArrayList<>();
        int slotId = 1;

        try {
            String[] startParts = startTimeStr.split(":");
            String[] endParts = endTimeStr.split(":");

            int startHour = Integer.parseInt(startParts[0]);
            int startMinute = Integer.parseInt(startParts[1]);
            int endHour = Integer.parseInt(endParts[0]);
            int endMinute = Integer.parseInt(endParts[1]);

            int currentMinutes = startHour * 60 + startMinute;
            int endMinutes = endHour * 60 + endMinute;

            Map<String, Integer> sessionCounters = new HashMap<>();
            sessionCounters.put("Morning", 1);
            sessionCounters.put("Afternoon", 1);
            sessionCounters.put("Evening", 1);

            while (currentMinutes + 20 <= endMinutes) {
                int startHr = currentMinutes / 60;
                int startMn = currentMinutes % 60;
                int endHr = (currentMinutes + 20) / 60;
                int endMn = (currentMinutes + 20) % 60;

                String session = getSession(startHr);
                int slotNumber = sessionCounters.getOrDefault(session, 1);
                sessionCounters.put(session, slotNumber + 1);

                String formattedStart = formatTime(startHr, startMn);
                String formattedEnd = formatTime(endHr, endMn);

                TimeSlot slot = new TimeSlot(slotId++, session + " Slot " + slotNumber,
                        formattedStart, formattedEnd, session);
                slots.add(slot);

                currentMinutes += 20;
            }

        } catch (Exception e) {
        }

        return slots;
    }

    private String getSession(int hour) {
        if (hour < 12) return "Morning";
        if (hour < 17) return "Afternoon";
        return "Evening";
    }

    private String formatTime(int h, int m) {
        String period = h >= 12 ? "PM" : "AM";
        int hour12 = h % 12 == 0 ? 12 : h % 12;
        return String.format("%02d:%02d %s", hour12, m, period);
    }
}