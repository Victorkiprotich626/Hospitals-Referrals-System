package com.example.referrals.web;

import com.example.referrals.reporting.TenantReportingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/viewer")
public class ViewerDashboardController {

    private final TenantReportingService reportingService;

    public ViewerDashboardController(TenantReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("report", reportingService.buildCurrentTenantReport());
        return "viewer/dashboard";
    }
}
