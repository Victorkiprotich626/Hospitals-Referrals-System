package com.example.referrals.web;

import com.example.referrals.superadmin.service.SuperAdminHospitalService;
import com.example.referrals.superadmin.service.SuperAdminUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/super-admin")
public class SuperAdminDashboardController {

    private final SuperAdminHospitalService hospitalService;
    private final SuperAdminUserService userService;

    public SuperAdminDashboardController(SuperAdminHospitalService hospitalService,
                                         SuperAdminUserService userService) {
        this.hospitalService = hospitalService;
        this.userService = userService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("hospitalCount", hospitalService.findAll().size());
        model.addAttribute("hospitalAdminCount", userService.findHospitalAdmins().size());
        return "superadmin/dashboard";
    }
}
