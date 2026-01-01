package com.example.doctor_patient_management_system.controller;

import com.example.doctor_patient_management_system.dto.DashboardStats;
import com.example.doctor_patient_management_system.dto.UserDto;
import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.User;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import com.example.doctor_patient_management_system.security.UserPrincipal;
import com.example.doctor_patient_management_system.service.AdminService;
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

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(defaultValue = "0") int userPage,  // Separate param for users
            @RequestParam(defaultValue = "10") int userSize,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String userSearch,
            // Appointment filters (separate)
            @RequestParam(defaultValue = "0") int appointmentPage,
            @RequestParam(defaultValue = "10") int appointmentSize,
            @RequestParam(required = false) AppointmentStatus statusFilter,
            @RequestParam(required = false) String appointmentSearch,
            Model model) {

        // Dashboard Stats (Fixed for CONFIRMED/CANCELLED)
        DashboardStats stats = adminService.getDashboardStats();
        model.addAttribute("stats", stats);

        // Users Table (Updated: Now Page<User>)
        Page<User> usersPage = adminService.getUsers(userPage, userSize, role, userSearch);
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("userCurrentPage", usersPage.getNumber());
        model.addAttribute("userTotalPages", usersPage.getTotalPages());
        model.addAttribute("userTotalElements", usersPage.getTotalElements());
        model.addAttribute("roleFilter", role != null ? role : "");
        model.addAttribute("userSearchQuery", userSearch != null ? userSearch : "");

        // Appointments Table (Fixed enum)
        Page<Appointment> appointmentsPage = adminService.getAppointments(appointmentPage, appointmentSize, statusFilter, appointmentSearch);
        model.addAttribute("appointments", appointmentsPage.getContent());
        model.addAttribute("appointmentCurrentPage", appointmentsPage.getNumber());
        model.addAttribute("appointmentTotalPages", appointmentsPage.getTotalPages());
        model.addAttribute("appointmentTotalElements", appointmentsPage.getTotalElements());
        model.addAttribute("statusFilter", statusFilter != null ? statusFilter.name() : "");
        model.addAttribute("appointmentSearchQuery", appointmentSearch != null ? appointmentSearch : "");

        // Charts Data (Fixed for enum)
        model.addAttribute("userGrowthData", adminService.getUserGrowthData());
        model.addAttribute("roleDistributionData", adminService.getRoleDistributionData());
        model.addAttribute("appointmentsTrendData", adminService.getAppointmentsTrendData());

        // Recent Activity (Fixed: Confirmed instead of Pending)
        model.addAttribute("recentRegistrations", adminService.getRecentRegistrations(5));
        model.addAttribute("recentAppointments", adminService.getRecentAppointments(5));
        model.addAttribute("confirmedAppointments", adminService.getConfirmedAppointments(5));  // Repurposed
        model.addAttribute("incompleteProfiles", adminService.getIncompleteProfilesCount());

        // Alerts (Fixed)
        model.addAttribute("alerts", adminService.getAlerts());

        return "admin/dashboard";
    }

    @PostMapping("/users/{id}/delete")
//    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            RedirectAttributes redirectAttributes) {

        try {
            // Prevent admin from deleting themselves
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
            return "admin/view-appointment";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Appointment not found: " + e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }


}










//@Controller
//@RequestMapping("/admin")
//@PreAuthorize("hasRole('ADMIN')")
//public class AdminController {
//
//    private final AdminService adminService;
//
//    public AdminController(AdminService adminService) {
//        this.adminService = adminService;
//    }
//
//    @GetMapping("/dashboard")
//    public String dashboard(
//            @RequestParam(required = false, defaultValue = "0") int page,
//            @RequestParam(required = false, defaultValue = "10") int size,
//            @RequestParam(required = false, defaultValue = "all") String tab,
//            @RequestParam(required = false) String search,
//            Model model) {
//
//        // Determine role filter
//        String roleFilter = "all".equals(tab) || tab == null ? null : tab.toUpperCase();
//
//        // Get paginated users
//        Page<UserDto> userPage = adminService.getUsers(page, size, roleFilter, search);
//
//        // Get stats (only once)
//        DashboardStats stats = adminService.getDashboardStats();
//        long incompleteProfiles = adminService.getIncompleteProfilesCount();
//
//        model.addAttribute("page", userPage);
//        model.addAttribute("users", userPage.getContent());
//        model.addAttribute("tab", roleFilter != null ? roleFilter : "all");
//        model.addAttribute("stats", stats);
//        model.addAttribute("currentSearch", search);
//
//        return "admin/dashboard";
//    }
//
//    @PostMapping("/users/{id}/role")
//    public String changeUserRole(
//            @PathVariable Long id,
//            @RequestParam String role,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            User user = adminService.changeUserRole(id, Role.valueOf(role.toUpperCase()));
//            redirectAttributes.addFlashAttribute("message",
//                    "Role updated to " + role + " for " + user.getEmail());
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error",
//                    "Failed to update role: " + e.getMessage());
//        }
//
//        return "redirect:/admin/dashboard";
//    }
//
//    @PostMapping("/users/{id}/delete")
//    public String deleteUser(
//            @PathVariable Long id,
//            @AuthenticationPrincipal UserPrincipal principal,
//            RedirectAttributes redirectAttributes) {
//
//        if (id.equals(principal.getUser().getId())) {
//            redirectAttributes.addFlashAttribute("error",
//                    "You cannot delete your own account!");
//            return "redirect:/admin/dashboard";
//        }
//
//        String result = adminService.deleteUser(id);
//        if (result.contains("successfully")) {
//            redirectAttributes.addFlashAttribute("message", result);
//        } else {
//            redirectAttributes.addFlashAttribute("error", result);
//        }
//
//        return "redirect:/admin/dashboard";
//    }
//}















//package com.example.doctor_patient_management_system.controller;
//
//import com.example.doctor_patient_management_system.model.User;
//import com.example.doctor_patient_management_system.model.enumeration.Role;
//import com.example.doctor_patient_management_system.repository.UserRepository;
//import com.example.doctor_patient_management_system.security.UserPrincipal;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Controller
//@RequestMapping("/admin")
//public class AdminController {
//
//    private final UserRepository userRepository;
//
//    public AdminController(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }
//
//    @GetMapping("/dashboard")
////    @PreAuthorize("hasRole('ADMIN')")
//    public String dashboard(
//            @RequestParam(required = false, defaultValue = "all") String tab,
//            @AuthenticationPrincipal UserPrincipal principal,
//            Model model) {
//
//        // Get all users or filter by role
//        List<User> users;
//        if ("all".equals(tab)) {
//            users = userRepository.findAll();
//        } else {
//            try {
//                Role role = Role.valueOf(tab);
//                users = userRepository.findAll().stream()
//                        .filter(u -> u.getRole() == role)
//                        .collect(Collectors.toList());
//            } catch (IllegalArgumentException e) {
//                users = userRepository.findAll();
//            }
//        }
//
//        // Calculate statistics
//        List<User> allUsers = userRepository.findAll();
//        long totalAdmins = allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count();
//        long totalDoctors = allUsers.stream().filter(u -> u.getRole() == Role.DOCTOR).count();
//        long totalPatients = allUsers.stream().filter(u -> u.getRole() == Role.PATIENT).count();
//        long incompleteProfiles = allUsers.stream().filter(u -> !u.isComplete()).count();
//
//        model.addAttribute("users", users);
//        model.addAttribute("tab", tab);
//        model.addAttribute("stats", Map.of(
//                "totalAdmins", totalAdmins,
//                "totalDoctors", totalDoctors,
//                "totalPatients", totalPatients,
//                "incompleteProfiles", incompleteProfiles
//        ));
//
//        return "admin/dashboard";
//    }
//
//
//
//
//
//    @PostMapping("/users/{id}/role")
////    @PreAuthorize("hasRole('ADMIN')")
//    public String changeUserRole(
//            @PathVariable Long id,
//            @RequestParam String role,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            User user = userRepository.findById(id)
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            Role newRole = Role.valueOf(role.toUpperCase());
//
//            // Don't allow changing your own role
//            // You can add additional validation here if needed
//
//            user.setRole(newRole);
//            user.setComplete(false); // Reset completion status when role changes
//            userRepository.save(user);
//
//            redirectAttributes.addFlashAttribute("message",
//                    "Role changed successfully for " + user.getEmail());
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error",
//                    "Failed to change role: " + e.getMessage());
//        }
//
//        return "redirect:/admin/dashboard";
//    }
//
//    @PostMapping("/users/{id}/delete")
////    @PreAuthorize("hasRole('ADMIN')")
//    public String deleteUser(
//            @PathVariable Long id,
//            @AuthenticationPrincipal UserPrincipal principal,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            User user = userRepository.findById(id)
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            // Don't allow admin to delete themselves
//            if (user.getId().equals(principal.getUser().getId())) {
//                redirectAttributes.addFlashAttribute("error",
//                        "You cannot delete your own account!");
//                return "redirect:/admin/dashboard";
//            }
//
//            String userEmail = user.getEmail();
//            userRepository.deleteById(id);
//
//            redirectAttributes.addFlashAttribute("message",
//                    "User deleted successfully: " + userEmail);
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error",
//                    "Failed to delete user: " + e.getMessage());
//        }
//
//        return "redirect:/admin/dashboard";
//    }
//}