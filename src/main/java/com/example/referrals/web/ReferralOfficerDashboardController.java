package com.example.referrals.web;

import com.example.referrals.hospitaladmin.service.HospitalAdminReferralService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/referral-officer")
public class ReferralOfficerDashboardController {

    private final HospitalAdminReferralService referralService;

    public ReferralOfficerDashboardController(HospitalAdminReferralService referralService) {
        this.referralService = referralService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("openReferralCount", referralService.countOpenForCurrentTenant());
        model.addAttribute("incomingAttentionCount", referralService.countIncomingAttentionForCurrentTenant());
        model.addAttribute("referrals", referralService.findAllVisibleToCurrentTenant());
        return "referralofficer/dashboard";
    }
}
