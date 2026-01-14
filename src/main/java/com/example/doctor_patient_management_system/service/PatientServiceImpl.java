package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.BookingMessage;
import com.example.doctor_patient_management_system.dto.PatientDto;
import com.example.doctor_patient_management_system.model.*;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PatientServiceImpl implements PatientService {

    private final RabbitProducerServiceImpl rabbitProducerService;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public PatientServiceImpl(RabbitProducerServiceImpl rabbitProducerService,
                              AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository,
                              UserRepository userRepository,
                              ReviewRepository reviewRepository) {
        this.rabbitProducerService = rabbitProducerService;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
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

    @Override
    public Patient findPatientByUserIdOrNull(Long userId) {
        return patientRepository.findByUserId(userId).orElse(null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "patient", key = "#userId"),
            @CacheEvict(value = "patientByUserId", key = "#userId")
    })
    public Patient createPatientProfile(Long userId, PatientDto dto, User user) {
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

        user.setComplete(true);
        userRepository.save(user);

        return patientRepository.save(patient);
    }

    @Override
    public Optional<Patient> getDoctorByUserId(Long userId) {
        return patientRepository.findByUserId(userId);
    }

    //ByUserId
    @Override
    @Cacheable(value = "patient", key = "#userId")
    public Optional<Patient> getPatientByUserId(Long userId) {
        //System.out.println("🔴 Patient Cache MISS: Fetching from DB for UserId=" + userId);  // Log for testing
        return patientRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Appointment bookAppointment(Long doctorId, Long patientId, LocalDate appointmentDate, String appointmentTime) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + patientId));


        boolean slotTaken = appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTime(
                doctorId, appointmentDate, appointmentTime);

        if (slotTaken) {
            throw new RuntimeException("This time slot is already booked");
        }

        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(appointmentDate);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        appointment = appointmentRepository.save(appointment);

        BookingMessage message = new BookingMessage();
        message.setAppointmentId(appointment.getId());
        message.setPatientEmail(patient.getPatientEmail());
        message.setPatientName(patient.getPatientName());
        message.setDoctorEmail(doctor.getEmail());
        message.setDoctorName(doctor.getDoctorName());
        message.setAppointmentDate(appointmentDate);
        message.setAppointmentTime(appointmentTime);
        message.setSpeciality(doctor.getSpeciality());
        message.setConsultationFee(doctor.getConsultationFee());

        rabbitProducerService.saveBookingMessage(message);

        return appointment;
    }

    @Override
    @Transactional
    public Review submitReview(Long appointmentId,Long patientId, Integer rating, String comment) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new RuntimeException("Access denied: Not your appointment");
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new RuntimeException("Can only review completed appointments");
        }

        if (reviewRepository.existsByAppointmentId(appointmentId)) {
            throw new RuntimeException("Review already submitted for this appointment");
        }

        Review review = new Review(appointment, rating, comment);
        return reviewRepository.save(review);
    }

    @Override
    public Optional<Review> getReviewByAppointmentId(Long appointmentId) {
        return reviewRepository.findByAppointmentId(appointmentId);
    }

    @Override
    public Patient gotPatientById(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found with ID: " + id));
    }

    @Override
    public Patient getByUserId(Long userId) {
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));
    }
}