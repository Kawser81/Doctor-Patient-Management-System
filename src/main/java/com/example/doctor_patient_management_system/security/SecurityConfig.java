package com.example.doctor_patient_management_system.security;

import com.example.doctor_patient_management_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtFilter, UserDetailsService userDetailsService,  UserRepository userRepository) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                // STATELESS - Session completely disabled
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/register", "/auth/logout").permitAll()
                        .requestMatchers("/favicon.ico", "/error").permitAll()
                        .requestMatchers("/", "/available", "/doctors", "/doctors/{id}/profile").permitAll()
                        .requestMatchers("/doctors/{id}/api/slots").permitAll()

                        .requestMatchers("/admin/dashboard").hasRole("ADMIN")
                        .requestMatchers("/doctors/profile", "/doctors/complete-registration",
                                "/doctors/{id}/edit", "/doctors/my-appointments/**").hasRole("DOCTOR")
                        .requestMatchers("/patients/**").hasRole("PATIENT")
                        .requestMatchers("/doctors/{id}/api/book").hasRole("PATIENT")

                        .anyRequest().authenticated()
                )

                .logout(logout -> logout.disable())

                .formLogin(form -> form.disable())

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}