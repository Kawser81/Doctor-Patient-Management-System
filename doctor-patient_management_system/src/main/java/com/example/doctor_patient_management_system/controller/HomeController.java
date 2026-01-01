package com.example.doctor_patient_management_system.controller;

import com.example.doctor_patient_management_system.service.DoctorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    private final DoctorService doctorService;

    public HomeController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }


    @GetMapping("/available")
    public String patientHome(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        // Get all available doctors
        List<DoctorService.AvailableDoctorSummary> allDoctors =
                doctorService.getAvailableDoctors(Integer.MAX_VALUE);

        // Calculate pagination
        int totalDoctors = allDoctors.size();
        int totalPages = (int) Math.ceil((double) totalDoctors / size);
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalDoctors);

        // Get doctors for current page
        List<DoctorService.AvailableDoctorSummary> currentPageDoctors =
                allDoctors.subList(startIndex, endIndex);

        model.addAttribute("availableDoctors", currentPageDoctors);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalDoctors", totalDoctors);
        model.addAttribute("pageSize", size);

        return "available";
    }


}
