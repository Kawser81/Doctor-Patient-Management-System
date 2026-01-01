package com.example.doctor_patient_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationMessage implements Serializable {
    private String userEmail;
    private String role;
    private String fullName;
}