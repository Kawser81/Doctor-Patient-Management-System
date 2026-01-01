package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.DashboardStats;
import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.Doctor;
import com.example.doctor_patient_management_system.model.Patient;
import com.example.doctor_patient_management_system.model.User;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import com.example.doctor_patient_management_system.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;

    public AdminService(DoctorRepository doctorRepository,
                        PatientRepository patientRepository,
                        AppointmentRepository appointmentRepository,
                        PrescriptionRepository prescriptionRepository,
                        UserRepository userRepository) {
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.userRepository = userRepository;
    }


    @Transactional
    public String deleteUser(Long id) {
        try {
            // Check if user exists
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

            String userEmail = user.getEmail();
            Role userRole = user.getRole();

            // Delete based on role with proper cascade handling
            switch (userRole) {
                case DOCTOR:
                    deleteDoctorAndRelatedData(id);
                    break;

                case PATIENT:
                    deletePatientAndRelatedData(id);
                    break;

                case ADMIN:
                    // Just delete admin user (no profile)
                    userRepository.deleteById(id);
                    break;
            }

            return "User deleted successfully: " + userEmail + " (" + userRole + ")";

        } catch (Exception e) {
            return "Failed to delete user: " + e.getMessage();
        }
    }

    @Transactional
    private void deleteDoctorAndRelatedData(Long userId) {
        // Find doctor profile
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElse(null);

        if (doctor != null) {
            Long doctorId = doctor.getId();

            // 1. Delete all prescriptions related to this doctor's appointments
            List<Appointment> doctorAppointments = appointmentRepository
                    .findByDoctorIdOrderByAppointmentDateDesc(doctorId);

            for (Appointment appt : doctorAppointments) {
                prescriptionRepository.findByAppointmentId(appt.getId())
                        .ifPresent(prescriptionRepository::delete);
            }

            // 2. Delete all appointments
            appointmentRepository.deleteByDoctorId(doctorId);

            // 3. Delete doctor availability overrides if exists
            // overrideRepository.deleteByDoctorId(doctorId); // If you have this

            // 4. Delete doctor profile
            doctorRepository.delete(doctor);
        }

        // 5. Finally delete user
        userRepository.deleteById(userId);
    }

    @Transactional
    private void deletePatientAndRelatedData(Long userId) {
        // Find patient profile
        Patient patient = patientRepository.findByUserId(userId)
                .orElse(null);

        if (patient != null) {
            // 1. Delete all prescriptions related to this patient's appointments
            List<Appointment> patientAppointments = appointmentRepository
                    .findByPatientIdOrderByAppointmentDateDesc(userId);

            for (Appointment appt : patientAppointments) {
                prescriptionRepository.findByAppointmentId(appt.getId())
                        .ifPresent(prescriptionRepository::delete);
            }

            // 2. Delete all appointments
            appointmentRepository.deleteByPatientId(userId);

            // 3. Delete patient profile
            patientRepository.delete(patient);
        }

        // 4. Finally delete user
        userRepository.deleteById(userId);
    }


    // Existing Methods (Users) - Fixed: Manual filtering to avoid ExampleMatcher/Specification issues
    public User changeUserRole(Long id, Role newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(newRole);
        user.setComplete(false);
        return userRepository.save(user);
    }

    public long getIncompleteProfilesCount() {
        return userRepository.countByCompleteFalse();
    }

    public DashboardStats getDashboardStats() {
        long admins = userRepository.countByRole(Role.ADMIN);
        long doctors = userRepository.countByRole(Role.DOCTOR);
        long patients = userRepository.countByRole(Role.PATIENT);
        long total = admins + doctors + patients;

        // Fixed: Manual count for CONFIRMED/CANCELLED (no PENDING/COMPLETED)
        List<Appointment> allAppts = appointmentRepository.findAll();
        long totalAppointments = allAppts.size();
        long confirmedAppointments = allAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
        long cancelledAppointments = allAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();

        return new DashboardStats(admins, doctors, patients, total, totalAppointments, confirmedAppointments, cancelledAppointments);
    }

    // Updated: Now returns Page<User> (direct, no UserDto mapping)
    public Page<User> getUsers(int page, int size, String role, String search) {
        // Load all users sorted by id descending
        List<User> allUsers = userRepository.findAll(Sort.by("id").descending());

        // Manual filtering (in-memory for dashboard; efficient for small datasets)
        List<User> filteredUsers = new ArrayList<>(allUsers);

        // Email search (containing, ignore case)
        if (search != null && !search.isBlank()) {
            filteredUsers = filteredUsers.stream()
                    .filter(u -> u.getEmail().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Role exact match (if provided)
        if (role != null && !role.isBlank()) {
            Role roleEnum = Role.valueOf(role.toUpperCase());
            filteredUsers = filteredUsers.stream()
                    .filter(u -> u.getRole() == roleEnum)
                    .collect(Collectors.toList());
        }

        // Manual pagination
        int start = page * size;
        int end = Math.min(start + size, filteredUsers.size());
        List<User> pagedUsers = start < filteredUsers.size() ? filteredUsers.subList(start, end) : new ArrayList<>();

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return new PageImpl<>(pagedUsers, pageable, filteredUsers.size());
    }

//    @Transactional
//    public String deleteUser(Long id) {
//        if (!userRepository.existsById(id)) {
//            return "User not found";
//        }
//        userRepository.deleteById(id);
//        return "User deleted successfully";
//    }

    // Fixed: Appointment Methods (Enum-limited, Manual Counts)
    public Page<Appointment> getAppointments(int page, int size, AppointmentStatus statusFilter, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        List<Appointment> appointments = appointmentRepository.findAll(pageable).getContent();

        // Filter by status (only CONFIRMED/CANCELLED)
        if (statusFilter != null) {
            appointments = appointments.stream()
                    .filter(a -> a.getStatus() == statusFilter)
                    .collect(Collectors.toList());
        }
        // Search
        if (search != null && !search.isBlank()) {
            appointments = appointments.stream()
                    .filter(a -> (a.getPatient() != null && a.getPatient().getEmail().toLowerCase().contains(search.toLowerCase())) ||
                            (a.getDoctor() != null && a.getDoctor().getDoctorName().toLowerCase().contains(search.toLowerCase())))
                    .collect(Collectors.toList());
        }

        // Manual pagination
        int start = page * size;
        int end = Math.min(start + size, appointments.size());
        List<Appointment> paged = start < appointments.size() ? appointments.subList(start, end) : new ArrayList<>();

        return new PageImpl<>(paged, pageable, appointments.size());
    }

    @Transactional
    public void cancelAppointment(Long id) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        appt.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appt);
    }


    public List<Appointment> getRecentAppointments(int limit) {
        return appointmentRepository.findAll(Sort.by("id").descending()).stream().limit(limit).collect(Collectors.toList());  // Use id as proxy for recent
    }

    public List<Appointment> getConfirmedAppointments(int limit) {  // Repurposed from pending
        List<Appointment> confirmed = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .sorted(Comparator.comparing(Appointment::getAppointmentDate).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        return confirmed;
    }

    // Fixed Charts (Use appointmentDate, Enum-limited)
    public List<Map<String, Object>> getUserGrowthData() {
        List<Map<String, Object>> data = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now.minusMonths(i));
            long count = userRepository.count();  // Placeholder; adjust if createdAt added
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            monthData.put("count", count);
            data.add(monthData);
        }
        return data;
    }

    public Map<String, Long> getRoleDistributionData() {
        Map<String, Long> data = new HashMap<>();
        data.put("Admin", userRepository.countByRole(Role.ADMIN));
        data.put("Doctor", userRepository.countByRole(Role.DOCTOR));
        data.put("Patient", userRepository.countByRole(Role.PATIENT));
        return data;
    }

    public List<Map<String, Object>> getAppointmentsTrendData() {
        // Fixed: Weekly using appointmentDate, only CONFIRMED/CANCELLED
        List<Map<String, Object>> data = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 3; i >= 0; i--) {
            LocalDate start = now.minusWeeks(i).with(DayOfWeek.MONDAY);
            LocalDate end = start.plusDays(6);
            List<Appointment> weekAppts = appointmentRepository.findAll().stream()
                    .filter(a -> a.getAppointmentDate().isAfter(start.minusDays(1)) && a.getAppointmentDate().isBefore(end.plusDays(1)))
                    .collect(Collectors.toList());
            long confirmed = weekAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
            long cancelled = weekAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
            Map<String, Object> weekData = new HashMap<>();
            weekData.put("week", "Week " + (i + 1));
            weekData.put("confirmed", confirmed);
            weekData.put("cancelled", cancelled);
            data.add(weekData);
        }
        return data;
    }

    public List<User> getRecentRegistrations(int limit) {
        return userRepository.findAll(Sort.by("id").descending()).stream().limit(limit).collect(Collectors.toList());  // Proxy for createdAt
    }

    // Fixed Alerts (Enum-based)
    public List<String> getAlerts() {
        List<String> alerts = new ArrayList<>();
        long incomplete = getIncompleteProfilesCount();
        if (incomplete > 0) {
            alerts.add("⚠️ " + incomplete + " incomplete profiles pending review.");
        }
        List<Appointment> allAppts = appointmentRepository.findAll();
        long confirmed = allAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
        long cancelled = allAppts.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
        if (cancelled > confirmed / 2) {  // Example alert
            alerts.add("🔔 High cancellation rate: " + cancelled + " cancelled vs " + confirmed + " confirmed.");
        }
        return alerts;
    }

    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + id));
    }


}













//package com.example.doctor_patient_management_system.service;
//
//import com.example.doctor_patient_management_system.dto.DashboardStats;
//import com.example.doctor_patient_management_system.model.User;
//import com.example.doctor_patient_management_system.dto.UserDto;
//import com.example.doctor_patient_management_system.model.enumeration.Role;
//import com.example.doctor_patient_management_system.repository.UserRepository;
//import jakarta.transaction.Transactional;
//import org.springframework.data.domain.*;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class AdminService {
//
//    private final UserRepository userRepository;
//
//    public AdminService(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }
//
//
//    public User changeUserRole(Long id, Role newRole) {
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//        user.setRole(newRole);
//        user.setComplete(false); // Role change হলে profile reset
//        return userRepository.save(user);
//    }
//
//    public long getIncompleteProfilesCount() {
//        return userRepository.countByCompleteFalse();
//    }
//
//
//    // 1. Get Dashboard Stats
//    public DashboardStats getDashboardStats() {
//        long admins = userRepository.countByRole(Role.ADMIN);
//        long doctors = userRepository.countByRole(Role.DOCTOR);
//        long patients = userRepository.countByRole(Role.PATIENT);
//        long total = admins + doctors + patients;
//
//        return new DashboardStats(admins, doctors, patients, total); // Works with record or class
//    }
//
//    // 2. Get Users with Pagination + Search + Role Filter
//    public Page<UserDto> getUsers(int page, int size, String role, String search) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
//
//        ExampleMatcher matcher = ExampleMatcher.matchingAny()
//                .withIgnoreCase()
//                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
//
//        User probe = new User();
//        if (search != null && !search.isBlank()) {
//            probe.setEmail(search);
//        }
//        if (role != null && !role.isBlank()) {
//            probe.setRole(Role.valueOf(role.toUpperCase()));
//        }
//
//        Example<User> example = Example.of(probe, matcher);
//        Page<User> userPage = userRepository.findAll(example, pageable);
//
//        List<UserDto> dtos = userPage.stream()
//                .map(u -> new UserDto(u.getId(), u.getEmail(), u.getRole().name()))
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(dtos, pageable, userPage.getTotalElements());
//    }
//
//    // 3. Delete User
//    @Transactional
//    public String deleteUser(Long id) {
//        if (!userRepository.existsById(id)) {
//            return "User not found";
//        }
//        userRepository.deleteById(id);
//        return "User deleted successfully";
//    }
//
//}
