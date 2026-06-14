package com.example.referrals.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication) {
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
            return "redirect:/super-admin";
        }
        if (hospitalAdmin) {
            return "redirect:/hospital-admin";
        }
        if (referralOfficer) {
            return "redirect:/referral-officer";
        }
        if (doctor) {
            return "redirect:/doctor";
        }
        if (viewer) {
            return "redirect:/viewer";
        }
        return "redirect:/login";
    }
}
