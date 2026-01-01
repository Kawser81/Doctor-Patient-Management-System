package com.example.doctor_patient_management_system.security;

import com.example.doctor_patient_management_system.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Arrays;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        String token = null;
        String username = null;

        // First, try to get token from Authorization header
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // If not in header, try to get from cookie
        if (token == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                token = Arrays.stream(cookies)
                        .filter(cookie -> "JWT_TOKEN".equals(cookie.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);
            }
        }

        // Validate token and extract username
        if (token != null) {
            try {
                if (jwtUtil.validateToken(token)) {
                    username = jwtUtil.getUsernameFromToken(token);
                }
            } catch (Exception e) {
                // Log the exception if needed
                System.out.println("JWT validation failed: " + e.getMessage());
            }
        }

        // Set authentication in context if valid
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtUtil.validateToken(token)) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}


























//@Component
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//    private final JwtUtil jwtUtil;
//    private final CustomUserDetailsService userDetailsService;
//
//    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
//        this.jwtUtil = jwtUtil;
//        this.userDetailsService = userDetailsService;
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain) throws ServletException, IOException {
//
//        System.out.println("JwtFilter triggered for: " + request.getRequestURI());
//        System.out.println("Method: " + request.getMethod());
//
//        String token = null;
//
//        // Header থেকে
//        String authHeader = request.getHeader("Authorization");
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            token = authHeader.substring(7);
//            System.out.println("Token from header: " + token);
//        }
//
//        // Cookie থেকে
//        if (token == null) {
//            Cookie[] cookies = request.getCookies();
//            if (cookies == null) {
//                System.out.println("Cookies are NULL!");
//            } else {
//                System.out.println("Total cookies: " + cookies.length);
//                for (Cookie cookie : cookies) {
//                    System.out.println("Cookie name: " + cookie.getName() + ", value: " + cookie.getValue());
//                    if ("JWT_TOKEN".equals(cookie.getName())) {
//                        token = cookie.getValue();
//                        System.out.println("Token from cookie: " + token);
//                    }
//                }
//            }
//        }
//
//        if (token != null && !token.isBlank()) {
//            try {
//                if (jwtUtil.validateToken(token)) {
//                    String username = jwtUtil.getUsernameFromToken(token);
//                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//                        UsernamePasswordAuthenticationToken auth =
//                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                        SecurityContextHolder.getContext().setAuthentication(auth);
//                        System.out.println("Authentication set for user: " + username);
//                    }
//                } else {
//                    System.out.println("Token validation failed");
//                }
//            } catch (Exception e) {
//                System.out.println("JWT exception: " + e.getMessage());
//                e.printStackTrace();
//            }
//        } else {
//            System.out.println("No token found");
//        }
//
//        filterChain.doFilter(request, response);
//    }
//}