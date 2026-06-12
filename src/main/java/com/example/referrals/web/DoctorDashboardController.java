package com.example.referrals.web;

import com.example.referrals.common.web.CurrentUserFacade;
import com.example.referrals.hospitaladmin.service.HospitalAdminReferralService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/doctor")
public class DoctorDashboardController {

    private final CurrentUserFacade currentUserFacade;
    private final HospitalAdminReferralService referralService;

    public DoctorDashboardController(CurrentUserFacade currentUserFacade,
                                     HospitalAdminReferralService referralService) {
        this.currentUserFacade = currentUserFacade;
        this.referralService = referralService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("fullName", currentUserFacade.requireUser().getFullName());
        model.addAttribute("assignedOpenCount", referralService.countAssignedOpenForCurrentDoctor());
        model.addAttribute("referrals", referralService.findAssignedToCurrentDoctor());
        return "doctor/dashboard";
    }
}
