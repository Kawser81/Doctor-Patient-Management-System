package com.example.doctor_patient_management_system.controller;

import com.example.doctor_patient_management_system.dto.AvailableDoctorSummary;
import com.example.doctor_patient_management_system.service.DoctorAvailabilityServiceImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    private final DoctorAvailabilityServiceImpl doctorAvailabilityService;

    public HomeController(DoctorAvailabilityServiceImpl doctorAvailabilityService) {
        this.doctorAvailabilityService = doctorAvailabilityService;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/available")
    public String getAvailableDoctors(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        List<AvailableDoctorSummary> allDoctors =
                doctorAvailabilityService.getAvailableDoctors(Integer.MAX_VALUE);

        int totalDoctors = allDoctors.size();
        int totalPages = (int) Math.ceil((double) totalDoctors / size);
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalDoctors);

        List<AvailableDoctorSummary> currentPageDoctors =
                allDoctors.subList(startIndex, endIndex);

        model.addAttribute("availableDoctors", currentPageDoctors);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalDoctors", totalDoctors);
        model.addAttribute("pageSize", size);

        return "available";
    }
}
