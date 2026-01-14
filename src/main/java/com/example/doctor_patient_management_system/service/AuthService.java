package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.AuthRequest;
import com.example.doctor_patient_management_system.dto.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuthService {

     AuthResponse register(@Valid @RequestBody AuthRequest request);

     AuthResponse login(AuthRequest request);

}
