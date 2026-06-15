package com.example.referrals.web;

import com.example.referrals.common.web.CurrentUserFacade;
import com.example.referrals.hospitaladmin.service.HospitalAdminDirectoryService;
import com.example.referrals.hospitaladmin.service.HospitalAdminPatientService;
import com.example.referrals.hospitaladmin.service.HospitalAdminReferralService;
import com.example.referrals.security.CustomUserDetails;
import com.example.referrals.superadmin.service.SuperAdminUserService;
import com.example.referrals.tenant.TenantContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/hospital-admin")
public class HospitalAdminDashboardController {

    private final CurrentUserFacade currentUserFacade;
    private final SuperAdminUserService userService;
    private final HospitalAdminDirectoryService directoryService;
    private final HospitalAdminPatientService patientService;
    private final HospitalAdminReferralService referralService;

    public HospitalAdminDashboardController(CurrentUserFacade currentUserFacade,
                                            SuperAdminUserService userService,
                                            HospitalAdminDirectoryService directoryService,
                                            HospitalAdminPatientService patientService,
                                            HospitalAdminReferralService referralService) {
        this.currentUserFacade = currentUserFacade;
        this.userService = userService;
        this.directoryService = directoryService;
        this.patientService = patientService;
        this.referralService = referralService;
    }

    @GetMapping
    public String dashboard(Model model) {
        CustomUserDetails user = currentUserFacade.requireUser();
        Long hospitalId = TenantContext.getTenantId();
        model.addAttribute("fullName", user.getFullName());
        model.addAttribute("tenantId", hospitalId);
        model.addAttribute("activeAdmins", hospitalId != null ? userService.countEnabledAdminsForHospital(hospitalId) : 0);
        model.addAttribute("departmentCount", directoryService.countEnabledDepartmentsForCurrentTenant());
        model.addAttribute("doctorCount", directoryService.countEnabledDoctorsForCurrentTenant());
        model.addAttribute("patientCount", patientService.countForCurrentTenant());
        model.addAttribute("openReferralCount", referralService.countOpenForCurrentTenant());
        model.addAttribute("incomingAttentionCount", referralService.countIncomingAttentionForCurrentTenant());
        return "hospitaladmin/dashboard";
    }
}
