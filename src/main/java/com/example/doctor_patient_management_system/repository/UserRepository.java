package com.example.doctor_patient_management_system.repository;

import com.example.doctor_patient_management_system.model.User;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);

    Page<User> findByRole(Role role, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = ?1")
    long countByRole(Role role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.complete = false")
    long countByCompleteFalse();

//    @Query("SELECT u FROM User u WHERE " +
//            "(:role IS NULL OR u.role = :role) AND " +
//            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
//    List<User> findByEmailSearchAndRole(@Param("search") String search, @Param("role") String role);
}
