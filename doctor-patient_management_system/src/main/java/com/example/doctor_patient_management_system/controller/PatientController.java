package com.example.doctor_patient_management_system.controller;

import com.example.doctor_patient_management_system.dto.BookingMessage;
import com.example.doctor_patient_management_system.dto.PatientDto;
import com.example.doctor_patient_management_system.dto.RegistrationMessage;
import com.example.doctor_patient_management_system.model.*;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import com.example.doctor_patient_management_system.repository.AppointmentRepository;
import com.example.doctor_patient_management_system.repository.PatientRepository;
import com.example.doctor_patient_management_system.repository.PrescriptionRepository;
import com.example.doctor_patient_management_system.repository.UserRepository;
import com.example.doctor_patient_management_system.security.JwtUtil;
import com.example.doctor_patient_management_system.security.UserPrincipal;
import com.example.doctor_patient_management_system.service.DoctorService;
import com.example.doctor_patient_management_system.service.PatientService;
import com.example.doctor_patient_management_system.service.RabbitProducerService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final JwtUtil jwtUtil;
    private final DoctorService doctorService;
    private final AppointmentRepository appointmentRepository;
    private final SpringTemplateEngine templateEngine;
    private final RabbitProducerService rabbitProducerService;

    public PatientController(PatientService patientService,
                             UserRepository userRepository,
                             PatientRepository patientRepository,
                             JwtUtil jwtUtil, DoctorService doctorService,
                             AppointmentRepository appointmentRepository,
                             SpringTemplateEngine templateEngine,
                             RabbitProducerService rabbitProducerService) {
        this.patientService = patientService;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.jwtUtil = jwtUtil;
        this.doctorService = doctorService;
        this.appointmentRepository = appointmentRepository;
        this.templateEngine = templateEngine;
        this.rabbitProducerService = rabbitProducerService;
    }


    @GetMapping("/complete-registration")
    public String showRegisterForm(@AuthenticationPrincipal UserPrincipal principal, Model model) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        User user = principal.getUser();

        if (user.getRole() != Role.PATIENT) {
            return "redirect:/";
        }

        if (user.isComplete()) {
            return "redirect:/patients/profile";
        }

        model.addAttribute("patientDto", new PatientDto());
        model.addAttribute("email", user.getEmail());
        return "complete-patient-registration";
    }

    @PostMapping("/complete-registration")
    @Transactional
    public String completePatientRegistration(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute("patientDto") PatientDto dto,
            BindingResult result,
            @RequestParam(value = "jwt", required = false) String jwtToken,
            HttpServletResponse response,
            Model model) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        User principalUser = principal.getUser();
        if (principalUser.getRole() != Role.PATIENT || principalUser.isComplete()) {
            return "redirect:/patients/profile";
        }

        String email = null;

        //extract email from it
        if (jwtToken != null && !jwtToken.isBlank()) {
            try {
                email = jwtUtil.getUsernameFromToken(jwtToken);
            } catch (Exception e) {
                email = null;
            }
        }

        // Fallback to authenticated user's email
        if (email == null || email.isBlank()) {
            email = principalUser.getEmail();
        }

        // Re-check user by email (defensive programming — exactly like doctor)
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.isComplete() || user.getRole() != Role.PATIENT) {
            return "redirect:/auth/login";
        }

        // Validation errors → re-render form
        if (result.hasErrors()) {
            model.addAttribute("patientDto", dto);
            model.addAttribute("email", email);
            if (jwtToken != null && !jwtToken.isBlank()) {
                model.addAttribute("jwt", jwtToken);
            }
            return "complete-patient-registration";
        }

        patientService.createPatientProfile(user.getId(), dto);
        user.setComplete(true);
        userRepository.save(user);

        // Generate new token after profile completion
        String newToken = jwtUtil.generateToken(email, user.getRole().name());
        response.addHeader("Authorization", "Bearer " + newToken);


        // ============ RabbitMQ Integration ============
        // Send profile completion notification
        RegistrationMessage profileCompletionMessage = new RegistrationMessage(
                user.getEmail(),
                "PATIENT",
                dto.getPatientName() // Now we have the full name
        );

        rabbitProducerService.saveRegistrationMessage(profileCompletionMessage);
        // ============================================


        return "redirect:/patients/profile";
    }

    @GetMapping("/profile")
    public String viewProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String speciality,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        User user = principal.getUser();

        if (!user.isComplete()) {
            return "redirect:/patients/complete-registration";
        }

//        Patient patient = patientRepository.findByUserId(user.getId())
//                .orElseThrow(() -> new RuntimeException("Patient profile not found"));

        //redis data pass
        Patient patient = patientService.getPatientByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));

        List<String> allSpecialities = doctorService.getAllSpecialitiesSorted();

        // Get all available doctors (filtered by speciality if provided)
        List<DoctorService.DoctorAvailabilitySummary> allAvailableDoctors =
                doctorService.getUpcomingAvailableFlatList(Integer.MAX_VALUE, speciality);

        // Calculate pagination
        int totalDoctors = allAvailableDoctors.size();
        int totalPages = (int) Math.ceil((double) totalDoctors / size);

        // Ensure page is within valid range
        if (page < 1) page = 1;
        if (page > totalPages && totalPages > 0) page = totalPages;

        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalDoctors);

        // Get doctors for current page
        List<DoctorService.DoctorAvailabilitySummary> currentPageDoctors =
                totalDoctors > 0 ? allAvailableDoctors.subList(startIndex, endIndex) : new ArrayList<>();

        // Add attributes to model
        model.addAttribute("patient", patient);
        model.addAttribute("allSpecialities", allSpecialities);
        model.addAttribute("selectedSpeciality", speciality);
        model.addAttribute("availableDoctors", currentPageDoctors);

        // Pagination attributes
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalDoctors", totalDoctors);
        model.addAttribute("pageSize", size);

        return "patients/profile";
    }


    @GetMapping("/appointments")
    public String myAppointments(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        if (principal == null) {
            return "redirect:/auth/login";
        }

        User user = principal.getUser();

        if (user.getRole() != Role.PATIENT) {
            return "redirect:/";
        }

        // Get all appointments where this user is the patient
        List<Appointment> appointments = appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(user.getId());

        model.addAttribute("appointments", appointments);
        model.addAttribute("pageTitle", "My Appointments");

        return "patients/appointments";
    }


    // API endpoint for updating profile
    @PreAuthorize("hasRole('PATIENT')")
    @PutMapping("/me")
    @ResponseBody
    public Patient updateMyProfile(@AuthenticationPrincipal UserPrincipal principal, @RequestBody PatientDto dto) {
        Long userId = principal.getUser().getId();
        return patientService.updatePatientProfile(userId, dto);
    }

    @GetMapping("/prescriptions/{appointmentId}")
    public String viewPrescription(@PathVariable Long appointmentId,
                                   @AuthenticationPrincipal UserPrincipal principal,
                                   Model model) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        // Security check
        if (!appointment.getPatient().getId().equals(principal.getUser().getId())) {
            return "redirect:/patients/appointments?access_denied";
        }

        // Manually load Patient entity using patient User id
        Patient patientProfile = patientRepository.findById(appointment.getPatient().getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));

        Prescription prescription = appointment.getPrescription();

        model.addAttribute("appointment", appointment);
        model.addAttribute("doctor", appointment.getDoctor());
        model.addAttribute("patient", patientProfile);
        model.addAttribute("prescription", prescription != null ? prescription : new Prescription());

        return "patients/view-prescription";
    }


    @GetMapping("/prescriptions/{appointmentId}/download-pdf")
    public ResponseEntity<byte[]> downloadPrescriptionPdf(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal UserPrincipal principal) throws Exception {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getPatient().getId().equals(principal.getUser().getId())) {
            throw new RuntimeException("Access denied");
        }

        Patient patient = patientRepository.findByUserId(appointment.getPatient().getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));

        Prescription prescription = appointment.getPrescription();

        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        context.setVariable("appointment", appointment);
        context.setVariable("doctor", appointment.getDoctor());
        context.setVariable("patient", patient);
        context.setVariable("prescription", prescription != null ? prescription : new Prescription());

        String html = templateEngine.process("patients/view-prescription-pdf", context);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(html, null);
        builder.toStream(baos);
        builder.run();

        byte[] pdfBytes = baos.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("prescription_" + appointmentId + ".pdf", StandardCharsets.UTF_8)
                .build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }


    @PostMapping("/appointments/{appointmentId}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseBody
    public ResponseEntity<?> cancelAppointment(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new RuntimeException("Appointment not found"));

            if (!appointment.getPatient().getId().equals(principal.getUser().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }

            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointmentRepository.save(appointment);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/book")
    public String bookAppointment(@RequestParam Long doctorId,
                                  @RequestParam LocalDate date,
                                  @RequestParam String time,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        patientService.bookAppointment(doctorId, principal.getUser().getId(), date, time);
        return "redirect:/patients/appointments?success";
    }

}