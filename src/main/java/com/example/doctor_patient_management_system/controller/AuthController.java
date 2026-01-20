package com.example.doctor_patient_management_system.controller;

import com.example.doctor_patient_management_system.dto.AuthRequest;
import com.example.doctor_patient_management_system.dto.DoctorDto;
import com.example.doctor_patient_management_system.dto.RegistrationMessage;
import com.example.doctor_patient_management_system.model.User;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import com.example.doctor_patient_management_system.security.JwtUtil;
import com.example.doctor_patient_management_system.service.RabbitProducerServiceImpl;
import com.example.doctor_patient_management_system.service.UserServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final RabbitProducerServiceImpl rabbitProducerService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserServiceImpl userService;

    public AuthController(PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          RabbitProducerServiceImpl rabbitProducerService,
                          UserServiceImpl userService) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.rabbitProducerService = rabbitProducerService;
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("request", new AuthRequest());
        return "auth/register";
    }


    @PostMapping("/register")
    @Transactional
    public String registerUser(
            @Valid @ModelAttribute("user") User user,
            BindingResult result,
            HttpServletResponse response,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            return "auth/register";
        }

        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email already registered!");
            return "auth/register";
        }

        try {
            User savedUser = userService.registerUser(user);

            String token = jwtUtil.generateToken(
                    savedUser.getEmail(),
                    savedUser.getRole().name()
            );

            Cookie cookie = new Cookie("JWT_TOKEN", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(request.isSecure());
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60);
            response.addCookie(cookie);

            RegistrationMessage registrationMessage = new RegistrationMessage(
                    savedUser.getEmail(),
                    savedUser.getRole().name(),
                    "New User"
            );
            rabbitProducerService.saveRegistrationMessage(registrationMessage);

            if (savedUser.getRole() == Role.DOCTOR) {
                return "redirect:/doctors/complete-registration";
            } else if (savedUser.getRole() == Role.PATIENT) {
                return "redirect:/patients/complete-registration";
            } else {
                return "redirect:/admin/dashboard";
            }

        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "auth/register";
        }
    }


    //Only for JWT uses
    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(required = false) String registered,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String error,
            Model model) {

        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && !(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {

            String role = SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities().stream()
                    .findFirst()
                    .get()
                    .getAuthority()
                    .replace("ROLE_", "");

            return "redirect:" + switch (role) {
                case "DOCTOR" -> "/doctors/profile";
                case "PATIENT" -> "/patients/profile";
                case "ADMIN" -> "/admin/dashboard";
                default -> "/";
            };
        }

        model.addAttribute("registered", registered);
        model.addAttribute("logout", logout);
        model.addAttribute("error", error);
        model.addAttribute("request", new AuthRequest());
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("request") AuthRequest request,
            BindingResult result,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("error", "Please fill all fields correctly");
            return "auth/login";
        }

        User user = userService.getUserByEmail(request.getEmail());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            model.addAttribute("error", "Invalid email or password");
            return "auth/login";
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        Cookie jwtCookie = new Cookie("JWT_TOKEN", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(24 * 60 * 60);
        response.addCookie(jwtCookie);

        response.addHeader("Authorization", "Bearer " + token);

        // Admin should ALWAYS go to dashboard, regardless of complete status
        if (user.getRole() == Role.ADMIN) {
            return "redirect:/admin/dashboard";
        }

        // For DOCTOR and PATIENT only, check if profile is complete
        if (!user.isComplete()) {
            if (user.getRole() == Role.DOCTOR) {
                model.addAttribute("doctorDto", new DoctorDto());
                model.addAttribute("email", user.getEmail());
                return "complete-doctor-registration";
            } else if (user.getRole() == Role.PATIENT) {
                return "redirect:/patients/complete-registration";
            }
        }

        String redirectUrl = switch (user.getRole()) {
            case DOCTOR -> "/doctors/profile";
            case PATIENT -> "/patients/profile";
            default -> "/";
        };

        return "redirect:" + redirectUrl;
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("JWT_TOKEN", null);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(request.isSecure());
        response.addCookie(jwtCookie);

        response.setHeader("Authorization", "");

        SecurityContextHolder.clearContext();

        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        } catch (Exception e) {
        }

        return "redirect:/auth/login?logout=true";
    }
}
