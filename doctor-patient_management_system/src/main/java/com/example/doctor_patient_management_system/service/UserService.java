package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.model.User;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import com.example.doctor_patient_management_system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Transactional
    public User registerUser(User user) {
        // Check if email already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setComplete(false);

        if (user.getRole() == null) {
            throw new RuntimeException("User role is required");
        }
        return userRepository.save(user);
    }

    //Authenticate user with email and password
    public User authenticate(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null; // Invalid password
        }

        return user;
    }

    //Find user by email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    //Find user by ID
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    //Check if email exists
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    //Get all users (for admin)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    //Update user
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    //Mark user profile as complete
    @Transactional
    public void markProfileComplete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setComplete(true);
        userRepository.save(user);
    }

    //Delete user by ID
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    //et user count by role
    public long countUsersByRole(Role role) {
        return userRepository.countByRole(role);
    }

    //Get incomplete profile count
    public long countIncompleteProfiles() {
        return userRepository.countByCompleteFalse();
    }

    //Change user password
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    //Reset password (for admin or forgot password)
    @Transactional
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}