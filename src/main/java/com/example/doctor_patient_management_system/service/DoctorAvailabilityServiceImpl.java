package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.AvailableDoctorSummary;
import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.Doctor;
import com.example.doctor_patient_management_system.model.DoctorAvailability;
import com.example.doctor_patient_management_system.model.DoctorAvailabilityOverride;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.repository.AppointmentRepository;
import com.example.doctor_patient_management_system.repository.DoctorAvailabilityOverrideRepository;
import com.example.doctor_patient_management_system.repository.DoctorAvailabilityRepository;
import com.example.doctor_patient_management_system.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorAvailabilityServiceImpl implements  DoctorAvailabilityService {

    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorAvailabilityOverrideRepository doctorAvailabilityOverrideRepository;

    public DoctorAvailabilityServiceImpl(DoctorAvailabilityRepository doctorAvailabilityRepository,
                                         DoctorRepository doctorRepository,
                                         AppointmentRepository appointmentRepository,
                                         DoctorAvailabilityOverrideRepository doctorAvailabilityOverrideRepository) {
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorAvailabilityOverrideRepository = doctorAvailabilityOverrideRepository;
    }


    @Override
    public Map<DayOfWeek, List<DoctorAvailability>> getAvailabilityByDay(Long doctorId) {
        List<DoctorAvailability> availabilities =
                doctorAvailabilityRepository.findByDoctorIdAndIsAvailableTrue(doctorId);

        return availabilities.stream()
                .sorted(Comparator.comparing(DoctorAvailability::getStartTime))
                .collect(Collectors.groupingBy(DoctorAvailability::getDayOfWeek));
    }

    @Override
    public void clearAvailability(Long doctorId) {
        List<DoctorAvailability> existing =
                doctorAvailabilityRepository.findByDoctorId(doctorId);
        doctorAvailabilityRepository.deleteAll(existing);
    }

    @Override
    public DoctorAvailability save(DoctorAvailability availability) {
        return doctorAvailabilityRepository.save(availability);
    }

    @Override
    public List<AvailableDoctorSummary> getAvailableDoctors(int limit) {
        List<Doctor> allDoctors = doctorRepository.findAll();

        List<AvailableDoctorSummary> results = allDoctors.stream()
                .filter(d -> hasAvailability(d.getId()))
                .map(d -> {
                    LocalDate nextDate = getNextAvailableDate(d.getId());
                    int slots = countAvailableSlotsNextWeek(d.getId());
                    return new AvailableDoctorSummary(d, nextDate, slots);
                })
                .sorted((a, b) -> a.getNextAvailableDate().compareTo(b.getNextAvailableDate()))
                .collect(Collectors.toList());

        if (limit == Integer.MAX_VALUE) {
            return results;
        }
        return results.stream().limit(limit).collect(Collectors.toList());
    }

    private boolean hasAvailability(Long doctorId) {
        LocalDate today = LocalDate.now();
        LocalDate endWeek = today.plusDays(7);
        List<Appointment> appts = appointmentRepository
                .findByDoctorIdAndAppointmentDateBetween(doctorId, today, endWeek);

        Map<LocalDate, List<Integer>> bookedMap = appts.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .collect(Collectors.groupingBy(Appointment::getAppointmentDate,
                        Collectors.mapping(Appointment::getSlotId, Collectors.toList())));

        for (LocalDate date = today; !date.isAfter(endWeek); date = date.plusDays(1)) {
            if (!isOffDay(date, doctorId) &&
                    (bookedMap.get(date) == null || bookedMap.get(date).size() < totalSlotsPerDay(doctorId))) {
                return true;
            }
        }
        return false;
    }

    private LocalDate getNextAvailableDate(Long doctorId) {
        LocalDate today = LocalDate.now();
        LocalDate endWeek = today.plusDays(7);
        List<Appointment> appts = appointmentRepository
                .findByDoctorIdAndAppointmentDateBetween(doctorId, today, endWeek);

        Map<LocalDate, List<Integer>> bookedMap = appts.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .collect(Collectors.groupingBy(Appointment::getAppointmentDate,
                        Collectors.mapping(Appointment::getSlotId, Collectors.toList())));

        for (LocalDate date = today; !date.isAfter(endWeek); date = date.plusDays(1)) {
            if (!isOffDay(date, doctorId) &&
                    (bookedMap.get(date) == null || bookedMap.get(date).size() < totalSlotsPerDay(doctorId))) {
                return date;
            }
        }
        return null;
    }

    private int countAvailableSlotsNextWeek(Long doctorId) {
        LocalDate today = LocalDate.now();
        LocalDate endWeek = today.plusDays(7);
        List<Appointment> appts = appointmentRepository
                .findByDoctorIdAndAppointmentDateBetween(doctorId, today, endWeek);

        Map<LocalDate, Long> bookedCountMap = appts.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .collect(Collectors.groupingBy(
                        Appointment::getAppointmentDate,
                        Collectors.counting()
                ));

        int totalAvailable = 0;
        for (LocalDate date = today; !date.isAfter(endWeek); date = date.plusDays(1)) {
            if (!isOffDay(date, doctorId)) {
                long bookedOnDay = bookedCountMap.getOrDefault(date, 0L);
                totalAvailable += totalSlotsPerDay(doctorId) - bookedOnDay;
            }
        }
        return totalAvailable;
    }

    @Override
    public boolean isOffDay(LocalDate date, Long doctorId) {
        Optional<DoctorAvailabilityOverride> override = doctorAvailabilityOverrideRepository.findByDoctorIdAndDate(doctorId, date);
        if (override.isPresent() && Boolean.FALSE.equals(override.get().getIsAvailable())) {
            return true;
        }

        Doctor doctor = doctorRepository.findById(doctorId).orElseThrow(() -> new RuntimeException("Doctor not found"));
        if (doctor.getOffDays() == null || doctor.getOffDays().isBlank()) {
            return false;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        String dayName = dayOfWeek.name();
        return Arrays.stream(doctor.getOffDays().split(","))
                .map(String::trim)
                .anyMatch(offDay -> offDay.equalsIgnoreCase(dayName));
    }

    @Override
    public int totalSlotsPerDay(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        String startStr = doctor.getConsultationStartTime();
        String endStr = doctor.getConsultationEndTime();

        if (startStr == null || endStr == null) {
            return 0;
        }

        LocalTime start = LocalTime.parse(startStr); // 09:00
        LocalTime end = LocalTime.parse(endStr);

        long minutes = java.time.Duration.between(start, end).toMinutes();
        if (minutes <= 0) return 0;

        int slotDurationMinutes = 20;
        return (int) (minutes / slotDurationMinutes);
    }


    //public record DoctorAvailabilitySummary(Doctor doctor, LocalDate nextAvailableDate) {}

    @Override
    public List<AvailableDoctorSummary> getUpcomingAvailableFlatList(int limit, String specialityFilter) {
        List<Doctor> candidates = doctorRepository.findAll();

        if (specialityFilter != null && !specialityFilter.isBlank()) {
            candidates = candidates.stream()
                    .filter(d -> specialityFilter.equalsIgnoreCase(d.getSpeciality()))
                    .collect(Collectors.toList());
        }

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(30);

        List<AvailableDoctorSummary> result = new ArrayList<>();
        Set<Long> added = new HashSet<>();

        for (LocalDate date = today; date.isBefore(endDate.plusDays(1)); date = date.plusDays(1)) {
            if (result.size() >= limit) break;

            for (Doctor doctor : candidates) {
                if (added.contains(doctor.getId())) continue;
                if (isOffDay(date, doctor.getId())) continue;

                List<Integer> booked = appointmentRepository.findBookedSlotIdsByDoctorIdAndDate(doctor.getId(), date);
                int total = totalSlotsPerDay(doctor.getId());

                if (total > 0 && booked.size() < total) {
                    result.add(new AvailableDoctorSummary(doctor, date));
                    added.add(doctor.getId());
                }
            }
        }

        return result;
    }

}