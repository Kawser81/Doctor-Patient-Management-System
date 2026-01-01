package com.example.doctor_patient_management_system.repository;

import com.example.doctor_patient_management_system.model.User;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);

    Page<User> findByRole(Role role, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = ?1")
    long countByRole(Role role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.complete = false")
    long countByCompleteFalse();
}