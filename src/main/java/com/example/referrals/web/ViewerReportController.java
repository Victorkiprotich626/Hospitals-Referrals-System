package com.example.referrals.web;

import com.example.referrals.reporting.TenantReportingService;
import com.example.referrals.referral.ReferralStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/viewer/reports")
public class ViewerReportController {

    private final TenantReportingService reportingService;

    public ViewerReportController(TenantReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping
    public String reports(@RequestParam(required = false) String q,
                          @RequestParam(required = false) ReferralStatus status,
                          Model model) {
        model.addAttribute("report", reportingService.buildCurrentTenantReport(q, status));
        model.addAttribute("q", q);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", ReferralStatus.values());
        return "viewer/reports";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<String> exportCsv(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) ReferralStatus status) {
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("tenant-referrals-report.csv", StandardCharsets.UTF_8)
                .build()
                .toString())
            .body(reportingService.buildCsvForCurrentTenant(q, status));
    }
}
