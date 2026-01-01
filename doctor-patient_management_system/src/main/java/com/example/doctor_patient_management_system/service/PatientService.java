package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.BookingMessage;
import com.example.doctor_patient_management_system.dto.PatientDto;
import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.Doctor;
import com.example.doctor_patient_management_system.model.Patient;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.repository.AppointmentRepository;
import com.example.doctor_patient_management_system.repository.DoctorRepository;
import com.example.doctor_patient_management_system.repository.PatientRepository;
import com.example.doctor_patient_management_system.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    private final RabbitProducerService rabbitProducerService;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public PatientService(RabbitProducerService rabbitProducerService, AppointmentRepository appointmentRepository, DoctorRepository doctorRepository, PatientRepository patientRepository, UserRepository userRepository) {
        this.rabbitProducerService = rabbitProducerService;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    @Cacheable(value = "patient", key = "#id")
    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "patient", key = "#userId"),
            @CacheEvict(value = "patientByUserId", key = "#userId")
    })
    public Patient updatePatientProfile(Long userId, PatientDto dto) {
        Patient patient = patientRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        patient.setPatientName(dto.getPatientName());
        patient.setPatientEmail(dto.getPatientEmail());
        patient.setGender(dto.getGender());
        patient.setContact(dto.getContact());

        return patientRepository.save(patient);

    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "patient", key = "#userId"),
            @CacheEvict(value = "patientByUserId", key = "#userId")
    })
    public Patient createPatientProfile(Long userId, PatientDto dto) {
        Optional<Patient> existingPatient = patientRepository.findById(userId);
        if(existingPatient.isPresent()){
            throw new RuntimeException("Patient already exists");
        }

        Patient patient = new Patient();
        patient.setPatientName(dto.getPatientName());
        patient.setPatientEmail(dto.getPatientEmail());
        patient.setGender(dto.getGender());
        patient.setContact(dto.getContact());
        patient.setUser(userRepository.findById(userId).orElseThrow());

        return patientRepository.save(patient);
    }

    public Optional<Patient> getDoctorByUserId(Long userId) {
        return patientRepository.findByUserId(userId);
    }

//    @Cacheable(value = "patientByUserId", key = "#userId")
//    public Optional<Patient> getPatientByUserId(Long userId) {
//        return patientRepository.findByUserId(userId);
//    }

    //ByUserId
    @Cacheable(value = "patient", key = "#userId")
    public Optional<Patient> getPatientByUserId(Long userId) {
        System.out.println("🔴 Patient Cache MISS: Fetching from DB for UserId=" + userId);  // Log for testing
        return patientRepository.findByUserId(userId);
    }


//    @Transactional
//    public Appointment bookAppointment(Long doctorId, Long patientId, LocalDate date, String timeSlot) {
//        // 1. Validation: slot available কিনা চেক
//        // 2. Appointment entity তৈরি করুন
//        Appointment appointment = new Appointment();
//        appointment.setDoctor(doctorRepository.findById(doctorId).orElseThrow());
//        appointment.setPatient(patientRepository.findById(patientId).orElseThrow());
//        appointment.setAppointmentDate(date);
//        appointment.setAppointmentTime(timeSlot);
//        appointment.setStatus(AppointmentStatus.CONFIRMED);
//
//        // 3. Save to DB
//        appointment = appointmentRepository.save(appointment);
//
//        // 4. RabbitMQ outbox-message save
//        BookingMessage message = new BookingMessage(
//                appointment.getId(),
//                appointment.getPatient().getPatientEmail(),
//                appointment.getPatient().getPatientName(),
//                appointment.getDoctor().getEmail(),
//                appointment.getDoctor().getDoctorName(),
//                appointment.getAppointmentDate(),
//                appointment.getAppointmentTime(),
//                appointment.getDoctor().getSpeciality(),
//                appointment.getDoctor().getConsultationFee()
//        );
//
//        rabbitProducerService.saveBookingMessage(message);
//
//        return appointment;
//    }


    @Transactional
    public Appointment bookAppointment(
            Long doctorId,
            Long patientId,
            LocalDate appointmentDate,
            String appointmentTime) {  // e.g., "14:00"

        // 1. Doctor এবং Patient লোড করুন
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + patientId));

        // 2. স্লট available কিনা চেক (ঐচ্ছিক, আপনার প্রজেক্টে যদি থাকে)
        // উদাহরণ: একই তারিখ + সময়ে অন্য অ্যাপয়েন্টমেন্ট আছে কিনা
        boolean slotTaken = appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTime(
                doctorId, appointmentDate, appointmentTime);

        if (slotTaken) {
            throw new RuntimeException("This time slot is already booked");
        }

//
//        Appointment appointment = new Appointment();
//        appointment.setDoctorId(doctorId);
//        appointment.setPatientId(patientId);  // <-- এই লাইন! setPatient(patient) না লিখে এটা লিখুন
//        appointment.setAppointmentDate(appointmentDate);
//        appointment.setAppointmentTime(appointmentTime);
//        appointment.setStatus(AppointmentStatus.CONFIRMED);
//
//        appointment = appointmentRepository.save(appointment);
//
//        // RabbitMQ মেসেজ — এখানে Patient ও Doctor entity লোড করে তথ্য নিন
////        Patient patient = patientRepository.findById(patientId).orElseThrow();
////        Doctor doctor = doctorRepository.findById(doctorId).orElseThrow();
//
//        BookingMessage message = new BookingMessage();
//        message.setAppointmentId(appointment.getId());
//        message.setPatientEmail(patient.getPatientEmail());
//        message.setPatientName(patient.getPatientName());
//        message.setDoctorEmail(doctor.getEmail());
//        message.setDoctorName(doctor.getDoctorName());
//        message.setAppointmentDate(appointmentDate);
//        message.setAppointmentTime(appointmentTime);
//        message.setSpeciality(doctor.getSpeciality());
//        message.setConsultationFee(doctor.getConsultationFee());




        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
//        appointment.setPatient(patient);
        appointment.setAppointmentDate(appointmentDate);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        appointment = appointmentRepository.save(appointment);

        // 4. RabbitMQ Outbox-এ মেসেজ সেভ (same transaction-এ → server crash-এ হারাবে না)
        BookingMessage message = new BookingMessage();
        message.setAppointmentId(appointment.getId());
        message.setPatientEmail(patient.getPatientEmail());
        message.setPatientName(patient.getPatientName());
        message.setDoctorEmail(doctor.getEmail());
        message.setDoctorName(doctor.getDoctorName());
        message.setAppointmentDate(appointmentDate);                    // LocalDate → @JsonFormat দিয়ে ঠিক হবে
        message.setAppointmentTime(appointmentTime);
        message.setSpeciality(doctor.getSpeciality());
        message.setConsultationFee(doctor.getConsultationFee());

        rabbitProducerService.saveBookingMessage(message);

        return appointment;
    }

}
