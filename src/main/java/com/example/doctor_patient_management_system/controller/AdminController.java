package com.example.doctor_patient_management_system.controller;

import com.example.doctor_patient_management_system.dto.DashboardStats;
import com.example.doctor_patient_management_system.dto.DoctorAppointmentDto;
import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.User;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.security.UserPrincipal;
import com.example.doctor_patient_management_system.service.AdminServiceImpl;
import com.example.doctor_patient_management_system.service.UserServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminServiceImpl adminService;

    public AdminController(AdminServiceImpl adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(defaultValue = "0") int userPage,
            @RequestParam(defaultValue = "10") int userSize,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String userSearch,
            @RequestParam(defaultValue = "0") int appointmentPage,
            @RequestParam(defaultValue = "10") int appointmentSize,
            @RequestParam(required = false) AppointmentStatus statusFilter,
            @RequestParam(required = false) String appointmentSearch,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        DashboardStats stats = adminService.getDashboardStats();
        model.addAttribute("stats", stats);

        Page<User> usersPage = adminService.getUsers(userPage, userSize, role, userSearch);
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("userCurrentPage", usersPage.getNumber());
        model.addAttribute("userTotalPages", usersPage.getTotalPages());
        model.addAttribute("userTotalElements", usersPage.getTotalElements());
        model.addAttribute("roleFilter", role != null ? role : "");
        model.addAttribute("userSearchQuery", userSearch != null ? userSearch : "");

        Page<DoctorAppointmentDto> appointmentsPage = adminService.getAppointments(
                appointmentPage, appointmentSize, statusFilter, appointmentSearch, startDate, endDate);
        model.addAttribute("appointments", appointmentsPage.getContent());
        model.addAttribute("appointmentCurrentPage", appointmentsPage.getNumber());
        model.addAttribute("appointmentTotalPages", appointmentsPage.getTotalPages());
        model.addAttribute("appointmentTotalElements", appointmentsPage.getTotalElements());
        model.addAttribute("statusFilter", statusFilter != null ? statusFilter.name() : "");
        model.addAttribute("appointmentSearchQuery", appointmentSearch != null ? appointmentSearch : "");
        model.addAttribute("startDate", startDate != null ? startDate : "");
        model.addAttribute("endDate", endDate != null ? endDate : "");

        model.addAttribute("recentRegistrations", adminService.getRecentRegistrations(5));
        model.addAttribute("recentAppointments", adminService.getRecentAppointments(5));
        model.addAttribute("confirmedAppointments", adminService.getConfirmedAppointments(5));
        model.addAttribute("incompleteProfiles", adminService.getIncompleteProfilesCount());

        model.addAttribute("alerts", adminService.getAlerts());

        return "admin/dashboard";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            RedirectAttributes redirectAttributes) {

        try {
            if (id.equals(principal.getUser().getId())) {
                redirectAttributes.addFlashAttribute("error",
                        "You cannot delete your own account!");
                return "redirect:/admin/dashboard";
            }

            String result = adminService.deleteUser(id);

            if (result.contains("successfully")) {
                redirectAttributes.addFlashAttribute("message", result);
            } else {
                redirectAttributes.addFlashAttribute("error", result);
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to delete user: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            adminService.cancelAppointment(id);
            redirectAttributes.addFlashAttribute("message",
                    "Appointment cancelled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to cancel appointment: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/appointments/{id}/view")
    public String viewAppointment(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Appointment appointment = adminService.getAppointmentById(id);
            model.addAttribute("appointment", appointment);

            if (appointment.getPrescription() != null) {
                model.addAttribute("prescription", appointment.getPrescription());
                model.addAttribute("doctor", appointment.getDoctor());
                model.addAttribute("patient", appointment.getPatient());
            }

            return "admin/view-appointment";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Appointment not found: " + e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

}