package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.UserDto;
import com.example.doctor_patient_management_system.model.Doctor;
import com.example.doctor_patient_management_system.model.User;
import com.example.doctor_patient_management_system.repository.DoctorRepository;
import com.example.doctor_patient_management_system.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PublicDoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    public PublicDoctorService(DoctorRepository doctorRepository, UserRepository userRepository) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
    }

    public List<Doctor> getAllDoctors(){
        return doctorRepository.findAll();
    }

    public Doctor getDoctor(Long id) {
        return doctorRepository.findById(id).orElseThrow();
    }

    public List<UserDto> findAllUser() {
        return userRepository.findAll().stream()
                .map(user -> new UserDto(user.getId(),user.getEmail(), user.getRole().name()))
                .toList();
    }
}
