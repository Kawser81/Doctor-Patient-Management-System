package com.example.doctor_patient_management_system.controller;

import com.example.doctor_patient_management_system.service.DoctorCalendarServiceImpl;
import com.example.doctor_patient_management_system.dto.*;
import com.example.doctor_patient_management_system.model.*;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import com.example.doctor_patient_management_system.security.JwtUtil;
import com.example.doctor_patient_management_system.security.UserPrincipal;
import com.example.doctor_patient_management_system.service.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/doctors")
public class DoctorController {

    private final RabbitProducerServiceImpl rabbitProducerService;
    private final DoctorServiceImpl doctorService;
    private final UserServiceImpl userService;
    private final JwtUtil jwtUtil;
    private final PatientServiceImpl patientService;
    private final PrescriptionServiceImpl prescriptionService;
    private final AppointmentServiceImpl appointmentService;
    private final DoctorCalendarServiceImpl doctorCalendarService;
    private final DoctorSlotServiceImpl doctorSlotService;


    public DoctorController(DoctorServiceImpl doctorService,
                            UserServiceImpl userService,
                            JwtUtil jwtUtil,
                            PatientServiceImpl patientService,
                            RabbitProducerServiceImpl rabbitProducerService,
                            PrescriptionServiceImpl prescriptionService,
                            AppointmentServiceImpl appointmentService,
                            DoctorCalendarServiceImpl doctorCalendarService,
                            DoctorSlotServiceImpl doctorSlotService) {
        this.doctorService = doctorService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.patientService = patientService;
        this.rabbitProducerService = rabbitProducerService;
        this.prescriptionService = prescriptionService;
        this.appointmentService = appointmentService;
        this.doctorCalendarService = doctorCalendarService;
        this.doctorSlotService = doctorSlotService;
    }

    @GetMapping("/{id}/profile")
    public String doctorProfile(@PathVariable Long id, Model model,
                                @RequestParam(defaultValue = "0") int year,
                                @RequestParam(defaultValue = "0") int month) {

        Doctor doctor = doctorService.findById(id);
        if (doctor == null) {
            return "redirect:/doctors";
        }

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

        YearMonth currentYm = YearMonth.of(year, month);
        model.addAttribute("prevYear", currentYm.minusMonths(1).getYear());
        model.addAttribute("prevMonth", currentYm.minusMonths(1).getMonthValue());
        model.addAttribute("nextYear", currentYm.plusMonths(1).getYear());
        model.addAttribute("nextMonth", currentYm.plusMonths(1).getMonthValue());

        List<DayStatus> calendarDays = doctorCalendarService.getCalendarData(id, year, month);
        model.addAttribute("calendarDays", calendarDays);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = auth != null && auth.getPrincipal() instanceof UserPrincipal
                ? ((UserPrincipal) auth.getPrincipal()).getUser() : null;

        boolean isDoctor = currentUser != null &&
                currentUser.getRole() == Role.DOCTOR &&
                currentUser.getId().equals(doctor.getId());

        boolean isPatient = currentUser != null && currentUser.getRole() == Role.PATIENT;

        model.addAttribute("isDoctor", isDoctor);
        model.addAttribute("isPatient", isPatient);

        List<ReviewDto> allReviews = doctorService.getReviewsForDoctor(id);
        model.addAttribute("allReviews", allReviews);
        Long reviewCount = doctorService.getReviewCountForDoctor(id);
        model.addAttribute("reviewCount", reviewCount);
        Double averageRating = doctorService.getAverageRatingForDoctor(id);
        model.addAttribute("averageRating", averageRating);

        List<ReviewDto> topReviews = allReviews.stream()
                .limit(3)
                .collect(Collectors.toList());
        model.addAttribute("topReviews", topReviews);

        return "doctors/profile";
    }

    @GetMapping("/{id}/api/slots")
    @ResponseBody
    public ResponseEntity<List<TimeSlot>> getAvailableSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            List<TimeSlot> slots = doctorSlotService.getAvailableSlots(id, date);
            return ResponseEntity.ok(slots);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @PostMapping("/{id}/api/book")
    @Transactional
    @ResponseBody
    public ResponseEntity<BookAppointmentResponse> bookAppointmentAPI(
            @PathVariable Long id,
            @RequestBody BookAppointmentRequest request,
            Authentication authentication) {

        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new BookAppointmentResponse(false, "Please login as a patient"));
            }

            User currentUser = ((UserPrincipal) authentication.getPrincipal()).getUser();
            if (currentUser.getRole() != Role.PATIENT) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new BookAppointmentResponse(false, "Only patients can book appointments"));
            }

            Doctor doctor = doctorService.getDoctorById(id);

            List<TimeSlot> slots = doctorSlotService.getAvailableSlots(id, request.getAppointmentDate());

            //TimeSlot: id, slotName, startTime, endTime, session;

            TimeSlot selectedSlot = slots.stream()
                    .filter(s -> s.getId() == request.getSlotId())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Slot not available"));

            String timeRange = selectedSlot.getStartTime() + " - " + selectedSlot.getEndTime();

            Appointment appointment = new Appointment(
                    doctor,
                    currentUser,
                    request.getSlotId(),
                    request.getAppointmentDate(),
                    timeRange
            );
            Appointment saved = appointmentService.bookAppointment(appointment);

            Patient patientProfile = patientService.findPatientByUserIdOrNull(currentUser.getId());

            String patientName = patientProfile != null ?
                    patientProfile.getPatientName() : currentUser.getEmail();

            BookingMessage bookingMessage = new BookingMessage(
                    saved.getId(),
                    currentUser.getEmail(),
                    patientName,
                    doctor.getEmail(),
                    doctor.getDoctorName(),
                    request.getAppointmentDate(),
                    timeRange,
                    doctor.getSpeciality(),
                    doctor.getConsultationFee()
            );

            rabbitProducerService.saveBookingMessage(bookingMessage);

            return ResponseEntity.ok(
                    new BookAppointmentResponse(true, "Booking confirmed successfully", saved.getId())
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BookAppointmentResponse(false, e.getMessage()));
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
            @RequestParam(required = false) String doctorName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        List<DoctorWithRatingDto> allDoctorsWithRatings = doctorService.findAllWithAverageRatings();

//        DoctorWithRatingDto {
//            doctor: Doctor { id: 1, doctorName: "Dr. Ahmed Rahman", speciality: "Cardiology", ... },
//            averageRating: 4.8,
//                    reviewCount: 15
//        }

        // Filter by speciality
        if (speciality != null && !speciality.isBlank()) {
            allDoctorsWithRatings = allDoctorsWithRatings.stream()
                    .filter(dto -> speciality.equalsIgnoreCase(dto.getDoctor().getSpeciality()))
                    .collect(Collectors.toList());
            model.addAttribute("selectedSpeciality", speciality);
        } else {
            model.addAttribute("selectedSpeciality", null);
        }

        // Filter by doctor name
        if (doctorName != null && !doctorName.isBlank()) {
            String searchTerm = doctorName.toLowerCase();
            allDoctorsWithRatings = allDoctorsWithRatings.stream()
                    .filter(dto -> dto.getDoctor()
                            .getDoctorName()
                            .toLowerCase()
                            .contains(searchTerm))
                    .collect(Collectors.toList());
            model.addAttribute("searchQuery", doctorName);
        }

        int totalDoctors = allDoctorsWithRatings.size();
        int totalPages = (int) Math.ceil((double) totalDoctors / size);
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalDoctors);

        List<DoctorWithRatingDto> currentPageDoctors = totalDoctors > 0
                ? allDoctorsWithRatings.subList(startIndex, endIndex)
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
        Doctor doctor = doctorService.findById(user.getId());
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

        Optional<User> currentUserOpt = userService.findUserByEmail(auth.getName());

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
        List<Appointment> appointments = appointmentService.findByDoctorIdOrderByAppointmentDateDesc(doctorId);

        List<DoctorAppointmentDto> appointmentDtos = new ArrayList<>();
        for (Appointment appointment : appointments) {
            Patient patientProfile = patientService.findPatientByUserIdOrNull(appointment.getPatient().getId());
            Prescription prescription = prescriptionService.getByAppointmentId(appointment.getId()).orElse(null);

            DoctorAppointmentDto dto = new DoctorAppointmentDto(appointment, patientProfile, prescription);
            appointmentDtos.add(dto);
        }

        model.addAttribute("items", appointmentDtos);
        return "doctors/my-appointments";
    }

    @GetMapping("/appointments/{appointmentId}/prescribe")
    @PreAuthorize("hasRole('DOCTOR')")
    public String showPrescribeForm(@PathVariable Long appointmentId,
                                    @AuthenticationPrincipal UserPrincipal principal,
                                    Model model) {
        Appointment appointment = appointmentService.getById(appointmentId);

        if (!appointment.getDoctor().getId().equals(principal.getUser().getId())) {
            return "redirect:/doctors/my-appointments";
        }

        Patient patient = patientService.findPatientByUserIdOrNull(appointment.getPatient().getId());
        Prescription prescription = prescriptionService.getByAppointmentId(appointmentId).orElse(null);

        if (prescription == null) {
            prescription = new Prescription();
        }
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

        Appointment appointment = appointmentService.getById(appointmentId);

        if (!appointment.getDoctor().getId().equals(principal.getUser().getId())) {
            return "redirect:/doctors/my-appointments";
        }

        if (result.hasErrors()) {
            return "doctors/prescribe";
        }

        prescriptionService.savePrescription(prescription, appointment);
        return "redirect:/doctors/my-appointments?prescription_saved";
    }

    @PostMapping("/appointments/{appointmentId}/cancel")
    @PreAuthorize("hasRole('DOCTOR')")
    @ResponseBody
    public ResponseEntity<CancelAppointmentResponse> cancelAppointment(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        try {
            Appointment appointment = appointmentService.getById(appointmentId);

            if (!appointment.getDoctor().getId().equals(principal.getUser().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new CancelAppointmentResponse("Access denied"));
            }

            if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new CancelAppointmentResponse("Only confirmed appointments can be cancelled"));
            }

            if (appointment.getPrescription() != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new CancelAppointmentResponse("Cannot cancel appointment after prescription has been provided"));
            }

            appointmentService.cancelAppointment(appointment);

            return ResponseEntity.ok(new CancelAppointmentResponse(true));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new CancelAppointmentResponse(e.getMessage()));
        }
    }

    @GetMapping("/appointments/{appointmentId}/view-prescription")
    @PreAuthorize("hasRole('DOCTOR')")
    public String viewPrescription(@PathVariable Long appointmentId,
                                   @AuthenticationPrincipal UserPrincipal principal,
                                   Model model) {

        Appointment appointment = appointmentService.getById(appointmentId);

        if (!appointment.getDoctor().getId().equals(principal.getUser().getId())) {
            return "redirect:/doctors/my-appointments?access_denied";
        }

        Patient patient = patientService.findPatientByUserIdOrNull(appointment.getPatient().getId());
        Prescription prescription = prescriptionService.getByAppointmentId(appointmentId).orElse(null);

        model.addAttribute("appointment", appointment);
        model.addAttribute("doctor", appointment.getDoctor());
        model.addAttribute("patient", patient);
        model.addAttribute("prescription", prescription);

        return "doctors/view-prescription";
    }

    @GetMapping("/{id}/block-days")
    @PreAuthorize("hasRole('DOCTOR')")
    public String manageBlocks(@PathVariable Long id, Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            return "redirect:/auth/login";
        }
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        if (!principal.getUser().getId().equals(id)) {
            return "redirect:/doctors/" + id + "/profile?access_denied";
        }

        List<DoctorAvailabilityOverride> blocks = doctorService.getUpcomingBlocks(id);
        model.addAttribute("blocks", blocks);
        model.addAttribute("doctorId", id);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("todayPlus30", LocalDate.now().plusDays(30));
        return "doctors/block-days";
    }

    @PostMapping("/{id}/block-day")
    @PreAuthorize("hasRole('DOCTOR')")
    @Transactional
    public String blockDay(@PathVariable Long id,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           RedirectAttributes ra) {

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

        if (date.isBefore(LocalDate.now())) {
            ra.addFlashAttribute("error", "Cannot block past dates");
            return "redirect:/doctors/" + id + "/block-days";
        }

        int cancelledCount = appointmentService.cancelConfirmedAppointmentsForDoctorOnDate(id, date);

        doctorService.blockDay(id, date);
        if (cancelledCount > 0) {
            ra.addFlashAttribute("warning",
                    "Day blocked successfully! " + cancelledCount +
                            " existing appointment(s) have been cancelled.");
        } else {
            ra.addFlashAttribute("success", "Day blocked successfully!");
        }

        return "redirect:/doctors/" + id + "/profile";
    }

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

    @GetMapping("/{id}/reviews")
    public String reviews(@PathVariable Long id, Model model,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size) {
        Doctor doctor = doctorService.findById(id);
        if (doctor == null) {
            return "redirect:/doctors";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewDto> reviewPage = doctorService.getReviewsForDoctorPaginated(id, pageable);

        model.addAttribute("reviews", reviewPage.getContent());
        model.addAttribute("doctor", doctor);
        model.addAttribute("averageRating", doctorService.getAverageRatingForDoctor(id));
        model.addAttribute("reviewCount", doctorService.getReviewCountForDoctor(id));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reviewPage.getTotalPages());
        model.addAttribute("totalElements", reviewPage.getTotalElements());

        model.addAttribute("pageSize", size);

        return "reviews";
    }
}
