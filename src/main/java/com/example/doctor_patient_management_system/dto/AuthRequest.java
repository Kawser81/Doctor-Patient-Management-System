package com.example.doctor_patient_management_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AuthRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "invalid email format")
    @Pattern(
            regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Email must have a valid domain (e.g., example.com)"
    )
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String role;

    public AuthRequest() {
    }

    public AuthRequest(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

}
