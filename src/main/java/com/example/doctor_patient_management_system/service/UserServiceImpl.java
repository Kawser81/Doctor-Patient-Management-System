package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.model.User;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import com.example.doctor_patient_management_system.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Object findAll() {
        return userRepository.findAll();
    }

    @Override
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
    @Override
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
    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    //Find user by ID
    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    //Check if email exists
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    //Get all users (for admin)
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    //Update user
    @Override
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    //Mark user profile as complete
    @Override
    @Transactional
    public void markProfileComplete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setComplete(true);
        userRepository.save(user);
    }

    //Delete user by ID
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    //et user count by role
    @Override
    public long countUsersByRole(Role role) {
        return userRepository.countByRole(role);
    }

    //Get incomplete profile count
    @Override
    public long countIncompleteProfiles() {
        return userRepository.countByCompleteFalse();
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

}