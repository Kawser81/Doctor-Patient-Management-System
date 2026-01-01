//package com.example.doctor_patient_management_system.controller;
//
//import com.example.doctor_patient_management_system.dto.UserDto;
//import com.example.doctor_patient_management_system.model.Doctor;
//import com.example.doctor_patient_management_system.repository.DoctorRepository;
//import com.example.doctor_patient_management_system.service.PublicDoctorService;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Objects;
//
//@Controller
//@RequestMapping("/public")
//public class PublicDoctorController {
//
//    private PublicDoctorService publicDoctorService;
//    private DoctorRepository doctorRepository;
//
//    public PublicDoctorController(PublicDoctorService publicDoctorService,  DoctorRepository doctorRepository) {
//        this.publicDoctorService = publicDoctorService;
//        this.doctorRepository = doctorRepository;
//    }
//
//    @GetMapping("/doctors")
//    public List<Doctor> listAllDoctors() {
//        return publicDoctorService.getAllDoctors();
//    }
//
//    @GetMapping("/doctor-list")
//    public String listDoctors(Model model) {
//        List<Doctor> doctors = doctorRepository.findAll();
//        model.addAttribute("doctors", doctors);
//        return "doctors/list";
//    }
//
//    @GetMapping("/doctor-list/{id}")
//    public String viewProfile(@PathVariable Long id, Model model) {
//        Doctor doctor = doctorRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Invalid doctor ID"));
//        model.addAttribute("doctor", doctor);
//        return "doctors/profile";
//    }
//
//    @GetMapping("/doctors/{id}")
//    public Doctor getDoctor(@PathVariable Long id) {
//        return publicDoctorService.getDoctor(id);
//    }
//
//    @GetMapping("/user")
//    public List<UserDto> getUsers() {
//        return publicDoctorService.findAllUser();
//    }
//
//
//    // for filtering
////    @GetMapping("/doc-list")
////    public String listDoctors(
////            @RequestParam(required = false) String speciality,
////            Model model) {
////
////        List<Doctor> doctors;
////        List<String> specialities = doctorRepository.findAll()
////                .stream()
////                .map(Doctor::getSpeciality)
////                .filter(Objects::nonNull)
////                .distinct()
////                .sorted()
////                .toList();
////
////        if (speciality != null && !speciality.isBlank()) {
////            doctors = doctorRepository.findBySpeciality(speciality);
////            model.addAttribute("selectedSpeciality", speciality);
////        } else {
////            doctors = doctorRepository.findAll();
////        }
////
////        model.addAttribute("doctors", doctors);
////        model.addAttribute("specialities", specialities);
////        return "doctors/list";
////    }
//
//}
