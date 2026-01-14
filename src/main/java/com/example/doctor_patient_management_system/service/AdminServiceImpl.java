package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.DashboardStats;
import com.example.doctor_patient_management_system.dto.DoctorAppointmentDto;
import com.example.doctor_patient_management_system.model.*;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import com.example.doctor_patient_management_system.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService{

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;

    public AdminServiceImpl(DoctorRepository doctorRepository,
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

    @Override
    @Transactional
    public String deleteUser(Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

            String userEmail = user.getEmail();
            Role userRole = user.getRole();

            switch (userRole) {
                case DOCTOR:
                    deleteDoctorAndRelatedData(id);
                    break;

                case PATIENT:
                    deletePatientAndRelatedData(id);
                    break;

                case ADMIN:
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
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElse(null);

        if (doctor != null) {
            Long doctorId = doctor.getId();

            List<Appointment> doctorAppointments = appointmentRepository
                    .findByDoctorIdOrderByAppointmentDateDesc(doctorId);

            for (Appointment appointment : doctorAppointments) {
                prescriptionRepository.findByAppointmentId(appointment.getId())
                        .ifPresent(prescriptionRepository::delete);
            }

            appointmentRepository.deleteByDoctorId(doctorId);
            doctorRepository.delete(doctor);
        }

        userRepository.deleteById(userId);
    }

    @Transactional
    private void deletePatientAndRelatedData(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElse(null);

        if (patient != null) {
            List<Appointment> patientAppointments = appointmentRepository
                    .findByPatientIdOrderByAppointmentDateDesc(userId);

            for (Appointment appointment : patientAppointments) {
                prescriptionRepository.findByAppointmentId(appointment.getId())
                        .ifPresent(prescriptionRepository::delete);
            }

            appointmentRepository.deleteByPatientId(userId);
            patientRepository.delete(patient);
        }

        userRepository.deleteById(userId);
    }

    @Override
    public User changeUserRole(Long id, Role newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(newRole);
        user.setComplete(false);
        return userRepository.save(user);
    }

    @Override
    public long getIncompleteProfilesCount() {
        return userRepository.countByCompleteFalse();
    }

    @Override
    public DashboardStats getDashboardStats() {
        long admins = userRepository.countByRole(Role.ADMIN);
        long doctors = userRepository.countByRole(Role.DOCTOR);
        long patients = userRepository.countByRole(Role.PATIENT);
        long total = admins + doctors + patients;

        List<Appointment> allAppointments = appointmentRepository.findAll();
        long totalAppointments = allAppointments.size();
        long confirmedAppointments = allAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
        long cancelledAppointments = allAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();

        return new DashboardStats(admins, doctors, patients, total, totalAppointments, confirmedAppointments, cancelledAppointments);
    }

    @Override
    public Page<User> getUsers(int page, int size, String role, String search) {

        List<User> allUsers = userRepository.findAll(Sort.by("id").descending());
        List<User> filteredUsers = new ArrayList<>(allUsers);

        if (search != null && !search.isBlank()) {
            filteredUsers = filteredUsers.stream()
                    .filter(u -> u.getEmail().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (role != null && !role.isBlank()) {
            Role roleEnum = Role.valueOf(role.toUpperCase());
            filteredUsers = filteredUsers.stream()
                    .filter(u -> u.getRole() == roleEnum)
                    .collect(Collectors.toList());
        }

        int start = page * size;
        int end = Math.min(start + size, filteredUsers.size());
        List<User> pagedUsers = start < filteredUsers.size() ? filteredUsers.subList(start, end) : new ArrayList<>();

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return new PageImpl<>(pagedUsers, pageable, filteredUsers.size());
    }

    @Override
    public Page<DoctorAppointmentDto> getAppointments(int page, int size, AppointmentStatus statusFilter,
                                                      String search, String startDate, String endDate) {
        List<Appointment> allAppointments = appointmentRepository.findAll(Sort.by("appointmentDate").descending()
                .and(Sort.by("id").descending()));

        if (statusFilter != null) {
            allAppointments = allAppointments.stream()
                    .filter(a -> a.getStatus() == statusFilter)
                    .collect(Collectors.toList());
        }

        if (startDate != null && !startDate.isBlank()) {
            try {
                LocalDate start = LocalDate.parse(startDate);
                allAppointments = allAppointments.stream()
                        .filter(a -> a.getAppointmentDate() != null &&
                                !a.getAppointmentDate().isBefore(start))
                        .collect(Collectors.toList());
            } catch (Exception e) {
            }
        }

        if (endDate != null && !endDate.isBlank()) {
            try {
                LocalDate end = LocalDate.parse(endDate);
                allAppointments = allAppointments.stream()
                        .filter(a -> a.getAppointmentDate() != null &&
                                !a.getAppointmentDate().isAfter(end))
                        .collect(Collectors.toList());
            } catch (Exception e) {
            }
        }

        if (search != null && !search.isBlank()) {
            allAppointments = allAppointments.stream()
                    .filter(a -> (a.getPatient() != null &&
                            a.getPatient().getEmail().toLowerCase().contains(search.toLowerCase())) ||
                            (a.getDoctor() != null &&
                                    a.getDoctor().getDoctorName().toLowerCase().contains(search.toLowerCase())))
                    .collect(Collectors.toList());
        }

        int startIdx = page * size;
        int endIdx = Math.min(startIdx + size, allAppointments.size());
        List<Appointment> pagedAppointments = startIdx < allAppointments.size()
                ? allAppointments.subList(startIdx, endIdx)
                : new ArrayList<>();

        List<DoctorAppointmentDto> dtos = pagedAppointments.stream().map(appointment -> {
            Patient patientProfile = patientRepository.findByUserId(appointment.getPatient().getId()).orElse(null);
            Prescription prescription = prescriptionRepository.findByAppointmentId(appointment.getId()).orElse(null);
            return new DoctorAppointmentDto(appointment, patientProfile, prescription);
        }).collect(Collectors.toList());

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("appointmentDate").descending().and(Sort.by("id").descending()));

        return new PageImpl<>(dtos, pageable, allAppointments.size());
    }

    @Override
    @Transactional
    public void cancelAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    @Override
    public List<Appointment> getRecentAppointments(int limit) {
        return appointmentRepository.findAll(Sort.by("id").descending()).stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<Appointment> getConfirmedAppointments(int limit) {
        List<Appointment> confirmed = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .sorted(Comparator.comparing(Appointment::getAppointmentDate).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        return confirmed;
    }

    @Override
    public List<User> getRecentRegistrations(int limit) {
        return userRepository.findAll(Sort.by("id").descending()).stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<String> getAlerts() {
        List<String> alerts = new ArrayList<>();
        long incomplete = getIncompleteProfilesCount();
        if (incomplete > 0) {
            alerts.add("⚠️ " + incomplete + " incomplete profiles pending review.");
        }
        List<Appointment> allAppointments = appointmentRepository.findAll();
        long confirmed = allAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
        long cancelled = allAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
        if (cancelled > confirmed / 2) {
            alerts.add("High cancellation rate: " + cancelled + " cancelled vs " + confirmed + " confirmed.");
        }
        return alerts;
    }

    @Override
    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + id));
    }
}