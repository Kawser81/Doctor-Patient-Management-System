package com.example.doctor_patient_management_system.controller;

import com.example.doctor_patient_management_system.dto.*;
import com.example.doctor_patient_management_system.model.*;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import com.example.doctor_patient_management_system.security.JwtUtil;
import com.example.doctor_patient_management_system.security.UserPrincipal;
import com.example.doctor_patient_management_system.service.*;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/patients")
public class PatientController {

    private final PatientServiceImpl patientService;
    private final JwtUtil jwtUtil;
    private final DoctorServiceImpl doctorService;
    private final SpringTemplateEngine templateEngine;
    private final RabbitProducerServiceImpl rabbitProducerService;
    private final UserServiceImpl userService;
    private final AppointmentServiceImpl appointmentService;
    private final DoctorAvailabilityServiceImpl doctorAvailabilityService;

    public PatientController(PatientServiceImpl patientService,
                             JwtUtil jwtUtil,
                             DoctorServiceImpl doctorService,
                             SpringTemplateEngine templateEngine,
                             RabbitProducerServiceImpl rabbitProducerService,
                             UserServiceImpl userService,
                             AppointmentServiceImpl appointmentService,
                             DoctorAvailabilityServiceImpl doctorAvailabilityService) {
        this.patientService = patientService;
        this.jwtUtil = jwtUtil;
        this.doctorService = doctorService;
        this.templateEngine = templateEngine;
        this.rabbitProducerService = rabbitProducerService;
        this.userService = userService;
        this.appointmentService = appointmentService;
        this.doctorAvailabilityService = doctorAvailabilityService;
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

        if (email == null || email.isBlank()) {
            email = principalUser.getEmail();
        }

        User user = userService.getUserByEmail(email);

        if (user == null || user.isComplete() || user.getRole() != Role.PATIENT) {
            return "redirect:/auth/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("patientDto", dto);
            model.addAttribute("email", email);
            if (jwtToken != null && !jwtToken.isBlank()) {
                model.addAttribute("jwt", jwtToken);
            }
            return "complete-patient-registration";
        }

        patientService.createPatientProfile(user.getId(), dto, user);

        String newToken = jwtUtil.generateToken(email, user.getRole().name());
        response.addHeader("Authorization", "Bearer " + newToken);


        // ============ RabbitMQ Integration ============
        // Send profile completion notification
        RegistrationMessage profileCompletionMessage = new RegistrationMessage(
                user.getEmail(),
                Role.PATIENT.name(),
                dto.getPatientName() // Now we have the full name
        );

        rabbitProducerService.saveRegistrationMessage(profileCompletionMessage);

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

        //redis data pass
        Patient patient = patientService.getPatientByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));

        List<String> allSpecialities = doctorService.getAllSpecialitiesSorted();

        List<AvailableDoctorSummary> allAvailableDoctors =
                doctorAvailabilityService.getUpcomingAvailableFlatList(Integer.MAX_VALUE, speciality);

        int totalDoctors = allAvailableDoctors.size();
        int totalPages = (int) Math.ceil((double) totalDoctors / size);

        if (page < 1) page = 1;
        if (page > totalPages && totalPages > 0) page = totalPages;

        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalDoctors);

        List<AvailableDoctorSummary> currentPageDoctors =
                totalDoctors > 0 ? allAvailableDoctors.subList(startIndex, endIndex) : new ArrayList<>();

        model.addAttribute("patient", patient);
        model.addAttribute("allSpecialities", allSpecialities);
        model.addAttribute("selectedSpeciality", speciality);
        model.addAttribute("availableDoctors", currentPageDoctors);

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

        List<Appointment> appointments = appointmentService.getPatientIdByAppointment(user.getId());

        model.addAttribute("appointments", appointments);
        model.addAttribute("pageTitle", "My Appointments");

        return "patients/appointments";
    }

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

        Appointment appointment = appointmentService.getById(appointmentId);

        if (!appointment.getPatient().getId().equals(principal.getUser().getId())) {
            return "redirect:/patients/appointments?access_denied";
        }

        Patient patientProfile = patientService.gotPatientById(appointment.getPatient().getId());
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

        Appointment appointment = appointmentService.getById(appointmentId);

        if (!appointment.getPatient().getId().equals(principal.getUser().getId())) {
            throw new RuntimeException("Access denied");
        }

        Patient patient = patientService.getByUserId(appointment.getPatient().getId());
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
    public ResponseEntity<CancelAppointmentResponse> cancelAppointment(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        try {
            Appointment appointment = appointmentService.getById(appointmentId);

            if (!appointment.getPatient().getId().equals(principal.getUser().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new CancelAppointmentResponse("Access denied"));
            }

            appointmentService.cancelAppointment(appointment);

            return ResponseEntity.ok(new CancelAppointmentResponse(true));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new CancelAppointmentResponse(e.getMessage()));
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

    @PostMapping("/reviews")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ReviewSubmitResponse> submitReview(
            @RequestParam Long appointmentId,
            @RequestParam @Min(1) @Max(5) Integer rating,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal UserPrincipal principal) {

        Review review = patientService.submitReview(appointmentId, principal.getUser().getId(), rating, comment);
        return ResponseEntity.ok(new ReviewSubmitResponse(true, review.getId(), null));
    }

}