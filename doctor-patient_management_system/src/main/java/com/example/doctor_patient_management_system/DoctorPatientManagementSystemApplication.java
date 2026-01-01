package com.example.doctor_patient_management_system;

import com.example.doctor_patient_management_system.service.DoctorService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class DoctorPatientManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(DoctorPatientManagementSystemApplication.class, args);
	}

	@Autowired
	private DoctorService doctorService;

//	@PostConstruct
//	public void init() {
//		doctorService.initSampleData(); // Runs on startup
//	}

}
