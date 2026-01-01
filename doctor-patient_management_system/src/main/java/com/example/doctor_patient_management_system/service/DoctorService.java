package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.controller.DoctorController;
import com.example.doctor_patient_management_system.dto.DoctorDto;
import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.Doctor;
import com.example.doctor_patient_management_system.model.DoctorAvailabilityOverride;
import com.example.doctor_patient_management_system.model.User;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import com.example.doctor_patient_management_system.repository.AppointmentRepository;
import com.example.doctor_patient_management_system.repository.DoctorAvailabilityOverrideRepository;
import com.example.doctor_patient_management_system.repository.DoctorRepository;
import com.example.doctor_patient_management_system.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;  // Add this
    private final AppointmentRepository appointmentRepository;
    private final DoctorAvailabilityOverrideRepository overrideRepo;

    public DoctorService(DoctorRepository doctorRepository,
                         UserRepository userRepository,
                         EntityManager entityManager,
                         AppointmentRepository appointmentRepository,
                         DoctorAvailabilityOverrideRepository overrideRepo) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
        this.appointmentRepository = appointmentRepository;
        this.overrideRepo = overrideRepo;
    }


    @Cacheable(value = "doctors", key = "'all'")
    public List<Doctor> findAll() {
        System.out.println("🔴 Cache MISS: Fetching all doctors from database");
        return doctorRepository.findAll();
    }


    public List<Doctor> findBySpeciality(String speciality) {
        return doctorRepository.findBySpecialityIgnoreCase(speciality);
    }

    public List<String> getAllSpecialitiesSorted() {
        return doctorRepository.findDistinctSpecialityOrderBySpecialityAsc();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "doctors", key = "'all'"),
            @CacheEvict(value = "doctor", key = "#userId"),
            @CacheEvict(value = "doctorByUserId", key = "#userId")
    })

    public Doctor createDoctorProfile(Long userId, DoctorDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isComplete()) {
            throw new RuntimeException("Profile already completed");
        }

        Doctor doctor = new Doctor();
        doctor.setDoctorName(dto.getDoctorName());
        doctor.setDegree(dto.getDegree());
        doctor.setSpeciality(dto.getSpeciality());
        doctor.setEmail(dto.getEmail() != null ? dto.getEmail() : user.getEmail());
        doctor.setAddress(dto.getAddress());
        doctor.setContact(dto.getContact());
        doctor.setConsultationStartTime(dto.getConsultationStartTime());
        doctor.setConsultationEndTime(dto.getConsultationEndTime());
        doctor.setOffDays(dto.getOffDays());
        doctor.setConsultationFee(dto.getConsultationFee());
        doctor.setUser(user);

        user.setComplete(true);

        // Use EntityManager to persist (not merge)
        entityManager.persist(doctor);
        entityManager.flush();

        return doctor;
    }


    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "doctors", key = "'all'"),
            @CacheEvict(value = "doctor", key = "#userId"),
            @CacheEvict(value = "doctorByUserId", key = "#userId")
    })
    public Doctor updateDoctorProfile(Long userId, DoctorDto dto) {
        Doctor doctor = doctorRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        doctor.setDoctorName(dto.getDoctorName());
        doctor.setEmail(dto.getEmail());
        doctor.setDegree(dto.getDegree());
        doctor.setSpeciality(dto.getSpeciality());
        doctor.setConsultationStartTime(dto.getConsultationStartTime());
        doctor.setConsultationEndTime(dto.getConsultationEndTime());
        doctor.setOffDays(dto.getOffDays());
        doctor.setConsultationFee(dto.getConsultationFee());
        doctor.setAddress(dto.getAddress());
        doctor.setContact(dto.getContact());

        Doctor updatedDoctor = doctorRepository.save(doctor);

        return updatedDoctor;

    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "doctors", key = "'all'"),
            @CacheEvict(value = "doctor", key = "#doctorId"),
            @CacheEvict(value = "doctorByUserId", allEntries = true)
    })
    public void deleteDoctor(Long doctorId) {
        doctorRepository.deleteById(doctorId);
        System.out.println("Doctor deleted, cache cleared");
    }


    public Doctor getDoctorById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + id));
    }

    public List<User> getAllUsersForAdmin() {
        return userRepository.findAll();
    }

//    @Cacheable(value = "doctorByUserId", key = "#userId")
//    public Optional<Doctor> getDoctorByUserId(Long userId) {
//        return doctorRepository.findByUserId(userId);
//    }

    @Cacheable(value = "doctor", key = "#id")
    public Doctor findById(Long id) {
        return doctorRepository.findById(id).orElseThrow(() -> new RuntimeException("Doctor not found"));
    }

    public Appointment bookAppointment(Appointment appointment) {
        List<Integer> bookedSlots = appointmentRepository.findBookedSlotIdsByDoctorIdAndDate(
                appointment.getDoctor().getId(), appointment.getAppointmentDate());

        if (bookedSlots.contains(appointment.getSlotId())) {
            throw new RuntimeException("Slot already booked");
        }
        return appointmentRepository.save(appointment);
    }

    public DoctorDto getDoctorEditDto(Long id, String email) {

        Doctor doctor = doctorRepository.findById(id).orElseThrow();
        Optional<User> currentUserOpt = userRepository.findByEmail(email);

        boolean canEdit = currentUserOpt.isPresent() &&
                (currentUserOpt.get().getId().equals(id) || currentUserOpt.get().getRole() == Role.ADMIN);

        if (!canEdit) {
            throw new AccessDeniedException("Cannot edit this profile");
        }

        DoctorDto dto = new DoctorDto();
        dto.setDoctorName(doctor.getDoctorName());
        dto.setEmail(doctor.getEmail());
        dto.setDegree(doctor.getDegree());
        dto.setSpeciality(doctor.getSpeciality());
        dto.setAddress(doctor.getAddress());
        dto.setContact(doctor.getContact());
        dto.setConsultationStartTime(doctor.getConsultationStartTime());
        dto.setConsultationEndTime(doctor.getConsultationEndTime());
        dto.setOffDays(doctor.getOffDays());
        dto.setConsultationFee(doctor.getConsultationFee());

        return dto;
    }


    // Server-side calendar data
    public static class DayStatus {
        private LocalDate date;
        private String status;
        private int bookedCount;
        private int availableCount;
        private boolean isToday;
        private String dayName;

        public DayStatus(LocalDate date, String status, int bookedCount, int availableCount, boolean isToday, String dayName) {
            this.date = date;
            this.status = status;
            this.bookedCount = bookedCount;
            this.availableCount = availableCount;
            this.isToday = isToday;
            this.dayName = dayName;
        }

        // Getters
        public LocalDate getDate() {
            return date;
        }

        public String getStatus() {
            return status;
        }

        public int getBookedCount() {
            return bookedCount;
        }

        public int getAvailableCount() {
            return availableCount;
        }

        public boolean isToday() {
            return isToday;
        }

        public String getDayName() {
            return dayName;
        }
    }

    public List<DayStatus> getCalendarData(Long doctorId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();
        LocalDate today = LocalDate.now();

        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndAppointmentDateBetween(doctorId, startDate, endDate);
        Map<LocalDate, List<Integer>> bookedSlotsMap = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .collect(Collectors.groupingBy(
                        Appointment::getAppointmentDate,
                        Collectors.mapping(Appointment::getSlotId, Collectors.toList())
                ));

        List<DayStatus> days = new ArrayList<>();
        List<LocalDate> allDates = IntStream.rangeClosed(1, endDate.getDayOfMonth())
                .mapToObj(day -> LocalDate.of(year, month, day))
                .collect(Collectors.toList());

        for (LocalDate date : allDates) {
            boolean isPast = date.isBefore(today);
            boolean isOff = isOffDay(date, doctorId);
            int bookedCount = (bookedSlotsMap.get(date) != null ? bookedSlotsMap.get(date).size() : 0);
            int totalSlots = totalSlotsPerDay(doctorId);
            int available = totalSlots - bookedCount;
            String status = isPast ? "past" : isOff ? "off" : bookedCount == 0 ? "available" : bookedCount < totalSlots ? "partial" : "full";
            boolean isTodayFlag = date.isEqual(today);
            String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            days.add(new DayStatus(date, status, bookedCount, available, isTodayFlag, dayName));
        }
        return days;
    }


    // Server-side slots
    public static class TimeSlot {
        private int id;
        private String slotName;
        private String startTime;
        private String endTime;
        private String session;

        public TimeSlot(int id, String slotName, String startTime, String endTime, String session) {
            this.id = id;
            this.slotName = slotName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.session = session;
        }

        // Getters
        public int getId() {
            return id;
        }

        public String getSlotName() {
            return slotName;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getSession() {
            return session;
        }
    }

    public List<TimeSlot> getAvailableSlots(Long doctorId, LocalDate date) {
        // Get booked slots for this date
        List<Integer> booked = appointmentRepository.findBookedSlotIdsByDoctorIdAndDate(doctorId, date);

        // Generate all slots
        List<TimeSlot> allSlots = generateAllTimeSlots(doctorId);

        // Filter out booked slots
        List<TimeSlot> availableSlots = allSlots.stream()
                .filter(slot -> !booked.contains(slot.getId()))
                .collect(Collectors.toList());

        return availableSlots;
    }

    private List<TimeSlot> generateAllTimeSlots(Long doctorId) {

        Doctor doctor = getDoctorById(doctorId);
        if (doctor == null) {
            return new ArrayList<>();
        }

        String startTimeStr = doctor.getConsultationStartTime();
        String endTimeStr = doctor.getConsultationEndTime();

        List<TimeSlot> slots = new ArrayList<>();
        int slotId = 1;

        try {
            // Parse time strings (format should be "HH:mm" like "09:00")
            String[] startParts = startTimeStr.split(":");
            String[] endParts = endTimeStr.split(":");

            int startHour = Integer.parseInt(startParts[0]);
            int startMinute = Integer.parseInt(startParts[1]);
            int endHour = Integer.parseInt(endParts[0]);
            int endMinute = Integer.parseInt(endParts[1]);

            int currentMinutes = startHour * 60 + startMinute;
            int endMinutes = endHour * 60 + endMinute;

            System.out.println("Start minutes: " + currentMinutes + ", End minutes: " + endMinutes);

            Map<String, Integer> sessionCounters = new HashMap<>();
            sessionCounters.put("Morning", 1);
            sessionCounters.put("Afternoon", 1);
            sessionCounters.put("Evening", 1);

            // Generate 20-minute slots
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

                System.out.println("Generated slot: ID=" + slot.getId() + ", " +
                        slot.getSlotName() + ", " + formattedStart + "-" + formattedEnd);

                currentMinutes += 20;
            }

            System.out.println("Total slots generated: " + slots.size());

        } catch (Exception e) {
            System.out.println("ERROR in generateAllTimeSlots: " + e.getMessage());
            e.printStackTrace();
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



    // Available doctors for patient home
    // Doctor next 1 week ar moddhe jotogulo slot available segulo show korbe
    public static class AvailableDoctorSummary {
        private Doctor doctor;
        private LocalDate nextAvailableDate;
        private int availableSlotsNextWeek;

        public AvailableDoctorSummary(Doctor doctor, LocalDate nextAvailableDate, int availableSlotsNextWeek) {
            this.doctor = doctor;
            this.nextAvailableDate = nextAvailableDate;
            this.availableSlotsNextWeek = availableSlotsNextWeek;
        }

        // Getters
        public Doctor getDoctor() {
            return doctor;
        }

        public LocalDate getNextAvailableDate() {
            return nextAvailableDate;
        }

        public int getAvailableSlotsNextWeek() {
            return availableSlotsNextWeek;
        }
    }


    private boolean hasAvailability(Long doctorId) {
        LocalDate today = LocalDate.now();
        LocalDate endWeek = today.plusDays(7);
        List<Appointment> appts = appointmentRepository.findByDoctorIdAndAppointmentDateBetween(doctorId, today, endWeek);
        Map<LocalDate, List<Integer>> bookedMap = appts.stream()
//                .filter(a -> "CONFIRMED".equals(a.getStatus()))
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .collect(Collectors.groupingBy(Appointment::getAppointmentDate,
                        Collectors.mapping(Appointment::getSlotId, Collectors.toList())));

        for (LocalDate date = today; !date.isAfter(endWeek); date = date.plusDays(1)) {
            if (!isOffDay(date, doctorId) && (bookedMap.get(date) == null || bookedMap.get(date).size() < totalSlotsPerDay(doctorId))) {
                return true;
            }
        }
        return false;
    }

    private LocalDate getNextAvailableDate(Long doctorId) {
        LocalDate today = LocalDate.now();
        LocalDate endWeek = today.plusDays(7);
        List<Appointment> appts = appointmentRepository.findByDoctorIdAndAppointmentDateBetween(doctorId, today, endWeek);
        Map<LocalDate, List<Integer>> bookedMap = appts.stream()
                .filter(a -> "CONFIRMED".equals(a.getStatus()))
                .collect(Collectors.groupingBy(Appointment::getAppointmentDate,
                        Collectors.mapping(Appointment::getSlotId, Collectors.toList())));

        for (LocalDate date = today; !date.isAfter(endWeek); date = date.plusDays(1)) {
            if (!isOffDay(date, doctorId) && (bookedMap.get(date) == null || bookedMap.get(date).size() < totalSlotsPerDay(doctorId))) {
                return date;
            }
        }
        return null;
    }

    private int countAvailableSlotsNextWeek(Long doctorId) {
        LocalDate today = LocalDate.now();
        LocalDate endWeek = today.plusDays(7);
        List<Appointment> appts = appointmentRepository.findByDoctorIdAndAppointmentDateBetween(doctorId, today, endWeek);
        Map<LocalDate, List<Integer>> bookedMap = appts.stream()
                .filter(a -> "CONFIRMED".equals(a.getStatus()))
                .collect(Collectors.groupingBy(Appointment::getAppointmentDate,
                        Collectors.mapping(Appointment::getSlotId, Collectors.toList())));

        int totalAvailable = 0;
        for (LocalDate date = today; !date.isAfter(endWeek); date = date.plusDays(1)) {
            if (!isOffDay(date, doctorId)) {
                int bookedOnDay = bookedMap.getOrDefault(date, List.of()).size();
                totalAvailable += totalSlotsPerDay(doctorId) - bookedOnDay;
            }
        }
        return totalAvailable;
    }


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

        // If limit is Integer.MAX_VALUE, return all, otherwise limit
        if (limit == Integer.MAX_VALUE) {
            return results;
        }
        return results.stream().limit(limit).collect(Collectors.toList());
    }


    public boolean isOffDay(LocalDate date, Long doctorId) {
        Optional<DoctorAvailabilityOverride> override = overrideRepo.findByDoctorIdAndDate(doctorId, date);
        if (override.isPresent() && Boolean.FALSE.equals(override.get().getIsAvailable())) {  // Use getIsAvailable()
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


    @Transactional
    public void blockDay(Long doctorId, LocalDate date) {
        Optional<DoctorAvailabilityOverride> existing = overrideRepo.findByDoctorIdAndDate(doctorId, date);
        DoctorAvailabilityOverride override = existing.orElse(new DoctorAvailabilityOverride());
        override.setDoctor(doctorRepository.findById(doctorId).orElseThrow());
        override.setOverrideDate(date);
        override.setIsAvailable(false);  // Block
        overrideRepo.save(override);
    }

    public List<DoctorAvailabilityOverride> getUpcomingBlocks(Long doctorId) {
        return overrideRepo.findUpcomingByDoctorId(doctorId, LocalDate.now());
    }


    private int totalSlotsPerDay(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        String startStr = doctor.getConsultationStartTime();
        String endStr = doctor.getConsultationEndTime();

        if (startStr == null || endStr == null) {
            return 0;
        }

        java.time.LocalTime start = java.time.LocalTime.parse(startStr);
        java.time.LocalTime end = java.time.LocalTime.parse(endStr);

        long minutes = java.time.Duration.between(start, end).toMinutes();
        if (minutes <= 0) return 0;

        // Each appointment slot = 20 minutes (change if different)
        int slotDurationMinutes = 20;
        return (int) (minutes / slotDurationMinutes);
    }

    public record DoctorAvailabilitySummary(Doctor doctor, LocalDate nextAvailableDate) {}

    public List<DoctorAvailabilitySummary> getUpcomingAvailableFlatList(int limit, String specialityFilter) {
        List<Doctor> candidates = doctorRepository.findAll();

        if (specialityFilter != null && !specialityFilter.isBlank()) {
            candidates = candidates.stream()
                    .filter(d -> specialityFilter.equalsIgnoreCase(d.getSpeciality()))
                    .collect(Collectors.toList());
        }

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(30);

        List<DoctorAvailabilitySummary> result = new ArrayList<>();
        Set<Long> added = new HashSet<>();

        for (LocalDate date = today; date.isBefore(endDate.plusDays(1)); date = date.plusDays(1)) {
            if (result.size() >= limit) break;

            for (Doctor doctor : candidates) {
                if (added.contains(doctor.getId())) continue;
                if (isOffDay(date, doctor.getId())) continue;

                List<Integer> booked = appointmentRepository.findBookedSlotIdsByDoctorIdAndDate(doctor.getId(), date);
                int total = totalSlotsPerDay(doctor.getId());

                if (total > 0 && booked.size() < total) {
                    result.add(new DoctorAvailabilitySummary(doctor, date));
                    added.add(doctor.getId());
                }
            }
        }

        return result;
    }


    @Transactional
    public void unblockDay(Long doctorId, LocalDate date) {
        Optional<DoctorAvailabilityOverride> existing = overrideRepo.findByDoctorIdAndDate(doctorId, date);
        if (existing.isPresent()) {
            overrideRepo.delete(existing.get());
        }
    }

}
