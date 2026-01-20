package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.DashboardStats;
import com.example.doctor_patient_management_system.dto.DoctorAppointmentDto;
import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.User;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.model.enumeration.Role;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminService {

     String deleteUser(Long id);

     //User changeUserRole(Long id, Role newRole);

     long getIncompleteProfilesCount();

     DashboardStats getDashboardStats();

     Page<User> getUsers(int page, int size, String role, String search);

     Page<DoctorAppointmentDto> getAppointments(int page, int size, AppointmentStatus statusFilter,
                                                      String search, String startDate, String endDate);

     void cancelAppointment(Long id);

     List<Appointment> getRecentAppointments(int limit);

     List<Appointment> getConfirmedAppointments(int limit);

     List<User> getRecentRegistrations(int limit);

     List<String> getAlerts();

     Appointment getAppointmentById(Long id);

}
