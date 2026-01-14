package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.model.User;
import com.example.doctor_patient_management_system.model.enumeration.Role;

import java.util.List;
import java.util.Optional;

public interface UserService {

     User registerUser(User user);

     User authenticate(String email, String password);

     Optional<User> findByEmail(String email);

     Optional<User> findById(Long id);

     boolean existsByEmail(String email);

     List<User> getAllUsers();

     User updateUser(User user);

     void markProfileComplete(Long userId);

     void deleteUser(Long userId);

     long countUsersByRole(Role role);

     long countIncompleteProfiles();

     User getUserByEmail(String email);

     Optional<User> findUserByEmail(String email);

}
