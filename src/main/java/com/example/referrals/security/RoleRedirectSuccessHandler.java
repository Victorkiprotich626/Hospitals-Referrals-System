package com.example.referrals.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleRedirectSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        boolean superAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN"));
        boolean hospitalAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_HOSPITAL_ADMIN"));
        boolean referralOfficer = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_REFERRAL_OFFICER"));
        boolean doctor = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_DOCTOR"));
        boolean viewer = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_VIEWER"));

        if (superAdmin) {
            response.sendRedirect("/super-admin");
            return;
        }
        if (hospitalAdmin) {
            response.sendRedirect("/hospital-admin");
            return;
        }
        if (referralOfficer) {
            response.sendRedirect("/referral-officer");
            return;
        }
        if (doctor) {
            response.sendRedirect("/doctor");
            return;
        }
        if (viewer) {
            response.sendRedirect("/viewer");
            return;
        }
        response.sendRedirect("/");
    }
}
