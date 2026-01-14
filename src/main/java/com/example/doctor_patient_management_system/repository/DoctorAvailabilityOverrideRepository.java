package com.example.doctor_patient_management_system.repository;

import com.example.doctor_patient_management_system.model.DoctorAvailabilityOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorAvailabilityOverrideRepository extends JpaRepository<DoctorAvailabilityOverride, Long> {

    @Query("SELECT o FROM DoctorAvailabilityOverride o WHERE o.doctor.id = :doctorId AND o.overrideDate = :date")
    Optional<DoctorAvailabilityOverride> findByDoctorIdAndDate(@Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    @Query("SELECT o FROM DoctorAvailabilityOverride o WHERE o.doctor.id = :doctorId AND o.overrideDate >= :fromDate")
    List<DoctorAvailabilityOverride> findUpcomingByDoctorId(@Param("doctorId") Long doctorId, @Param("fromDate") LocalDate fromDate);

    // Optional: Custom delete method if needed for unblock (can use native deleteById, but this is more specific)
    @Query("DELETE FROM DoctorAvailabilityOverride o WHERE o.doctor.id = :doctorId AND o.overrideDate = :date")
    void deleteByDoctorIdAndDate(@Param("doctorId") Long doctorId, @Param("date") LocalDate date);
}