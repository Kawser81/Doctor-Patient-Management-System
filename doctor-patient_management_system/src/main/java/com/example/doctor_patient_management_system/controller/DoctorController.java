package com.example.doctor_patient_management_system.controller;

import com.example.doctor_patient_management_system.dto.BookingMessage;
import com.example.doctor_patient_management_system.dto.DoctorDto;
import com.example.doctor_patient_management_system.dto.RegistrationMessage;
import com.example.doctor_patient_management_system.model.*;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import com.example.doctor_patient_management_system.repository.*;
import com.example.doctor_patient_management_system.security.JwtUtil;
import com.example.doctor_patient_management_system.security.UserPrincipal;
import com.example.doctor_patient_management_system.service.DoctorAvailabilityService;
import com.example.doctor_patient_management_system.service.DoctorService;
import com.example.doctor_patient_management_system.service.RabbitProducerService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Controller
@RequestMapping("/doctors")
public class DoctorController {

    private final DoctorAvailabilityOverrideRepository overrideRepo;
    private final RabbitProducerService rabbitProducerService;
    private final DoctorService doctorService;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public DoctorController(DoctorAvailabilityOverrideRepository overrideRepo,
                            DoctorService doctorService,
                            DoctorRepository doctorRepository,
                            UserRepository userRepository,
                            JwtUtil jwtUtil,
                            AppointmentRepository appointmentRepository,
                            PrescriptionRepository prescriptionRepository,
                            PatientRepository patientRepository,
                            RabbitProducerService rabbitProducerService) {
        this.doctorService = doctorService;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.rabbitProducerService = rabbitProducerService;
        this.overrideRepo = overrideRepo;
    }


    @GetMapping("/{id}/profile")
    public String doctorProfile(@PathVariable Long id, Model model,
                                @RequestParam(defaultValue = "0") int year,
                                @RequestParam(defaultValue = "0") int month) {

        //redis data pass
        Doctor doctor = doctorService.findById(id);
        if (doctor == null) {
            return "redirect:/doctors";
        }

        // Set default to current month if not provided
        if (year == 0 || month == 0) {
            YearMonth current = YearMonth.now();
            year = current.getYear();
            month = current.getMonthValue();
        }

        model.addAttribute("doctor", doctor);
        model.addAttribute("currentYear", year);
        model.addAttribute("currentMonth", month);
        model.addAttribute("monthName", YearMonth.of(year, month).getMonth()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH));

        // Month navigation
        YearMonth currentYm = YearMonth.of(year, month);
        model.addAttribute("prevYear", currentYm.minusMonths(1).getYear());
        model.addAttribute("prevMonth", currentYm.minusMonths(1).getMonthValue());
        model.addAttribute("nextYear", currentYm.plusMonths(1).getYear());
        model.addAttribute("nextMonth", currentYm.plusMonths(1).getMonthValue());

        // CRITICAL: Pass calendar data
        List<DoctorService.DayStatus> calendarDays = doctorService.getCalendarData(id, year, month);
        model.addAttribute("calendarDays", calendarDays);

        // Role check
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = auth != null && auth.getPrincipal() instanceof UserPrincipal
                ? ((UserPrincipal) auth.getPrincipal()).getUser() : null;

        // ADD THIS: Check if current user is THIS doctor
        boolean isDoctor = currentUser != null &&
                currentUser.getRole() == Role.DOCTOR &&
                currentUser.getId().equals(doctor.getId());

        boolean isPatient = currentUser != null && currentUser.getRole() == Role.PATIENT;

        model.addAttribute("isDoctor", isDoctor);  // ADD THIS LINE
        model.addAttribute("isPatient", isPatient);

        return "doctors/profile";
    }

    @GetMapping("/{id}/api/slots")
    @ResponseBody
    public ResponseEntity<?> getAvailableSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            // Add detailed logging
            System.out.println("=== GET SLOTS REQUEST ===");
            System.out.println("Doctor ID: " + id);
            System.out.println("Date: " + date);

            List<DoctorService.TimeSlot> slots = doctorService.getAvailableSlots(id, date);

            System.out.println("Slots found: " + slots.size());
            slots.forEach(slot -> {
                System.out.println("Slot: " + slot.getId() + " | " +
                        slot.getSlotName() + " | " +
                        slot.getStartTime() + " - " + slot.getEndTime() +
                        " | Session: " + slot.getSession());
            });

            // Convert to DTO format to ensure proper JSON serialization
            List<Map<String, Object>> slotDtos = slots.stream()
                    .map(slot -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", slot.getId());
                        dto.put("slotName", slot.getSlotName());
                        dto.put("startTime", slot.getStartTime());
                        dto.put("endTime", slot.getEndTime());
                        dto.put("session", slot.getSession());
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(slotDtos);

        } catch (Exception e) {
            System.err.println("ERROR in getAvailableSlots: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to load slots: " + e.getMessage()));
        }
    }


    @PostMapping("/{id}/api/book")
    @Transactional
    @ResponseBody
    public ResponseEntity<?> bookAppointmentAPI(
            @PathVariable Long id,
            @RequestBody Map<String, Object> bookingData,
            Authentication authentication) {

        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Please login as a patient"));
            }

            User currentUser = ((UserPrincipal) authentication.getPrincipal()).getUser();
            if (currentUser.getRole() != Role.PATIENT) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only patients can book appointments"));
            }

            // Get doctor
            Doctor doctor = doctorService.getDoctorById(id);

            // Parse booking data
            int slotId = ((Number) bookingData.get("slotId")).intValue();
            LocalDate date = LocalDate.parse((String) bookingData.get("appointmentDate"));

            // Get slot info
            List<DoctorService.TimeSlot> slots = doctorService.getAvailableSlots(id, date);
            DoctorService.TimeSlot selectedSlot = slots.stream()
                    .filter(s -> s.getId() == slotId)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Slot not available"));

            String timeRange = selectedSlot.getStartTime() + " - " + selectedSlot.getEndTime();

            // Create and save appointment
            Appointment appointment = new Appointment(doctor, currentUser, slotId, date, timeRange);
            Appointment saved = doctorService.bookAppointment(appointment);

            // Get patient profile
            Patient patientProfile = patientRepository.findByUserId(currentUser.getId())
                    .orElse(null);

            String patientName = patientProfile != null ?
                    patientProfile.getPatientName() : currentUser.getEmail();

            // Create booking message
            BookingMessage bookingMessage = new BookingMessage(
                    saved.getId(),
                    currentUser.getEmail(),
                    patientName,
                    doctor.getEmail(),
                    doctor.getDoctorName(),
                    date,
                    timeRange,
                    doctor.getSpeciality(),
                    doctor.getConsultationFee()
            );

            // Save to outbox instead of sending directly
            rabbitProducerService.saveBookingMessage(bookingMessage);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "appointmentId", saved.getId(),
                    "message", "Booking confirmed successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/complete-registration")
    public String showCompleteForm(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        if (principal == null || principal.getUser() == null) {
            return "redirect:/auth/login";
        }

        User user = principal.getUser();
        if (user.getRole() != Role.DOCTOR || user.isComplete()) {
            return "redirect:/doctors/profile";
        }

        DoctorDto dto = new DoctorDto();
        dto.setEmail(user.getEmail());
        model.addAttribute("doctorDto", dto);
        model.addAttribute("email", user.getEmail());
        return "complete-doctor-registration";
    }

    @PostMapping("/complete-registration")
    @Transactional
    public String completeRegistration(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute("doctorDto") DoctorDto dto,
            BindingResult result,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (principal == null || principal.getUser() == null) {
            return "redirect:/auth/login";
        }

        User user = principal.getUser();

        if (user.getRole() != Role.DOCTOR || user.isComplete()) {
            return "redirect:/doctors/profile";
        }

        if (result.hasErrors()) {
            model.addAttribute("email", user.getEmail());
            return "complete-doctor-registration";
        }

        try {
            entityManager.clear();
            doctorService.createDoctorProfile(user.getId(), dto);

            String newToken = jwtUtil.generateToken(user.getEmail(), "DOCTOR");

            Cookie cookie = new Cookie("JWT_TOKEN", newToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(request.isSecure());
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60);
            response.addCookie(cookie);


            // ============ RabbitMQ Integration ============
            RegistrationMessage profileCompletionMessage = new RegistrationMessage(
                    user.getEmail(),
                    "DOCTOR",
                    dto.getDoctorName()
            );

            rabbitProducerService.saveRegistrationMessage(profileCompletionMessage);

            redirectAttributes.addFlashAttribute("success", "Profile created successfully!");
            return "redirect:/doctors/profile";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "There was an error while creating the profile! Try again");
            model.addAttribute("email", user.getEmail());
            return "complete-doctor-registration";
        }
    }


    @GetMapping
    public String listDoctors(
            @RequestParam(required = false) String speciality,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        List<Doctor> allDoctors;

        if (speciality == null || speciality.isBlank()) {
            // Spring Cache automatically handles caching
            allDoctors = doctorService.findAll();
            model.addAttribute("selectedSpeciality", null);
        } else {
            allDoctors = doctorService.findBySpeciality(speciality);
            model.addAttribute("selectedSpeciality", speciality);
        }

        // Pagination logic
        int totalDoctors = allDoctors.size();
        int totalPages = (int) Math.ceil((double) totalDoctors / size);
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalDoctors);

        List<Doctor> currentPageDoctors = totalDoctors > 0
                ? allDoctors.subList(startIndex, endIndex)
                : new ArrayList<>();

        model.addAttribute("doctors", currentPageDoctors);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalDoctors", totalDoctors);
        model.addAttribute("pageSize", size);

        List<String> allSpecialities = doctorService.getAllSpecialitiesSorted();
        model.addAttribute("allSpecialities", allSpecialities);

        return "doctors/list";
    }


    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        User user = principal.getUser();
        if (!user.isComplete()) {
            return "redirect:/doctors/complete-registration";
        }
        Doctor doctor = doctorRepository.findByUserId(user.getId())
                .orElseThrow();

        return "redirect:/doctors/" + doctor.getId() + "/profile";
    }

    @GetMapping("/{id}/edit")
    public String editProfile(@PathVariable Long id, Model model, Authentication auth) {

        DoctorDto dto = doctorService.getDoctorEditDto(id, auth.getName());

        model.addAttribute("doctorDto", dto);
        model.addAttribute("doctorId", id);
        return "doctors/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateProfile(
            @PathVariable Long id,
            @Valid @ModelAttribute("doctorDto") DoctorDto dto,
            BindingResult result,
            Authentication auth,
            Model model) {

        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/auth/login?required";
        }

        // Re-check authorization
        Optional<User> currentUserOpt = userRepository.findByEmail(auth.getName());
        boolean canEdit = currentUserOpt.isPresent() &&
                (currentUserOpt.get().getId().equals(id) || currentUserOpt.get().getRole() == Role.ADMIN);

        if (!canEdit) {
            return "redirect:/doctors/" + id + "/profile?access_denied";
        }

        if (result.hasErrors()) {
            model.addAttribute("doctorId", id);
            return "doctors/edit";
        }

        try {
            doctorService.updateDoctorProfile(id, dto);
            return "redirect:/doctors/" + id + "/profile?success";
        } catch (Exception e) {
            result.reject("globalError", "An error occurred: " + e.getMessage());
            model.addAttribute("doctorId", id);
            return "doctors/edit";
        }
    }


    @GetMapping("/my-appointments")
    @PreAuthorize("hasRole('DOCTOR')")
    public String myAppointments(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        Long doctorId = principal.getUser().getId();
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdOrderByAppointmentDateDesc(doctorId);
        List<Map<String, Object>> appointmentWithPatient = new ArrayList<>();

        for (Appointment appt : appointments) {
            Patient patientProfile = patientRepository.findByUserId(appt.getPatient().getId())
                    .orElse(null);

            // Fetch prescription for this appointment
            Prescription prescription = prescriptionRepository.findByAppointmentId(appt.getId())
                    .orElse(null);

            Map<String, Object> item = new HashMap<>();
            item.put("appointment", appt);
            item.put("patientProfile", patientProfile);
            item.put("prescription", prescription);  // ADD THIS LINE

            appointmentWithPatient.add(item);
        }

        model.addAttribute("items", appointmentWithPatient);
        return "doctors/my-appointments";
    }


    @GetMapping("/appointments/{appointmentId}/prescribe")
    @PreAuthorize("hasRole('DOCTOR')")
    public String showPrescribeForm(@PathVariable Long appointmentId,
                                    @AuthenticationPrincipal UserPrincipal principal,
                                    Model model) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getDoctor().getId().equals(principal.getUser().getId())) {
            return "redirect:/doctors/my-appointments";
        }

        Patient patient = patientRepository.findByUserId(appointment.getPatient().getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));

        Prescription prescription = prescriptionRepository.findByAppointmentId(appointmentId)
                .orElse(new Prescription());

        prescription.setAppointment(appointment);

        model.addAttribute("appointment", appointment);
        model.addAttribute("patient", patient);
        model.addAttribute("prescription", prescription);

        return "doctors/prescribe";
    }

    @PostMapping("/appointments/{appointmentId}/prescribe")
    @PreAuthorize("hasRole('DOCTOR')")
    public String savePrescription(@PathVariable Long appointmentId,
                                   @Valid @ModelAttribute Prescription prescription,
                                   BindingResult result,
                                   @AuthenticationPrincipal UserPrincipal principal) {

        Appointment appointment = appointmentRepository.findById(appointmentId).get();

        if (!appointment.getDoctor().getId().equals(principal.getUser().getId())) {
            return "redirect:/doctors/my-appointments";
        }

        if (result.hasErrors()) {
            return "doctors/prescribe";
        }

        prescription.setAppointment(appointment);
        prescription.setCreatedAt(LocalDateTime.now());
        prescriptionRepository.save(prescription);

        return "redirect:/doctors/my-appointments?prescription_saved";
    }


    @PostMapping("/appointments/{appointmentId}/cancel")
    @PreAuthorize("hasRole('DOCTOR')")
    @ResponseBody
    public ResponseEntity<?> cancelAppointment(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new RuntimeException("Appointment not found"));

            if (!appointment.getDoctor().getId().equals(principal.getUser().getId())) {
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

    @GetMapping("/appointments/{appointmentId}/view-prescription")
    @PreAuthorize("hasRole('DOCTOR')")
    public String viewPrescription(@PathVariable Long appointmentId,
                                   @AuthenticationPrincipal UserPrincipal principal,
                                   Model model) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getDoctor().getId().equals(principal.getUser().getId())) {
            return "redirect:/doctors/my-appointments?access_denied";
        }

        // Get patient profile
        Patient patient = patientRepository.findByUserId(appointment.getPatient().getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));

        // Get prescription
        Prescription prescription = prescriptionRepository.findByAppointmentId(appointmentId)
                .orElse(new Prescription());

        model.addAttribute("appointment", appointment);
        model.addAttribute("doctor", appointment.getDoctor());
        model.addAttribute("patient", patient);
        model.addAttribute("prescription", prescription);

        return "doctors/view-prescription";
    }


    // GET: Management page (form to block/unblock days) - URL: /doctors/{id}/block-days
    @GetMapping("/{id}/block-days")  // Note: plural "days"
    @PreAuthorize("hasRole('DOCTOR')")
    public String manageBlocks(@PathVariable Long id, Model model) {
        // Check if the current user owns this doctor profile
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            return "redirect:/auth/login";  // Or handle unauthorized
        }
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        if (!principal.getUser().getId().equals(id)) {
            return "redirect:/doctors/" + id + "/profile?access_denied";  // Prevent editing others
        }

        List<DoctorAvailabilityOverride> blocks = doctorService.getUpcomingBlocks(id);
        model.addAttribute("blocks", blocks);
        model.addAttribute("doctorId", id);
        // Optional: Add current date for form limits
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("todayPlus30", LocalDate.now().plusDays(30));
        return "doctors/block-days";  // Matches templates/doctors/block-days.html
    }

    // POST: Block a day - Called from form submit
    @PostMapping("/{id}/block-day")  // Singular "day"
    @PreAuthorize("hasRole('DOCTOR')")
    @Transactional
    public String blockDay(@PathVariable Long id,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           RedirectAttributes ra) {
        // Authorization check (same as above)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            ra.addFlashAttribute("error", "Please login");
            return "redirect:/auth/login";
        }
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        if (!principal.getUser().getId().equals(id)) {
            ra.addFlashAttribute("error", "Access denied");
            return "redirect:/doctors/" + id + "/profile";
        }

        // Validate date (future only)
        if (date.isBefore(LocalDate.now())) {
            ra.addFlashAttribute("error", "Cannot block past dates");
            return "redirect:/doctors/" + id + "/block-days";
        }

        doctorService.blockDay(id, date);
        ra.addFlashAttribute("success", "Day blocked successfully!");
        return "redirect:/doctors/" + id + "/profile";
    }

    // POST: Unblock a day - Called from table buttons
    @PostMapping("/{id}/unblock-day")
    @PreAuthorize("hasRole('DOCTOR')")
    @Transactional
    public String unblockDay(@PathVariable Long id,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             RedirectAttributes ra) {

        doctorService.unblockDay(id, date);
        ra.addFlashAttribute("success", "Day unblocked successfully!");
        return "redirect:/doctors/" + id + "/block-days";
    }


}