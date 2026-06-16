package com.example.referrals.web;

import com.example.referrals.common.model.RoleName;
import com.example.referrals.hospitaladmin.form.HospitalUserForm;
import com.example.referrals.hospitaladmin.service.HospitalAdminDirectoryService;
import com.example.referrals.hospitaladmin.service.HospitalAdminStaffService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/hospital-admin/users")
public class HospitalAdminUserController {

    private final HospitalAdminStaffService staffService;
    private final HospitalAdminDirectoryService directoryService;

    public HospitalAdminUserController(HospitalAdminStaffService staffService,
                                       HospitalAdminDirectoryService directoryService) {
        this.staffService = staffService;
        this.directoryService = directoryService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) RoleName role,
                       Model model) {
        model.addAttribute("users", staffService.findManagedUsersForCurrentTenant(q, role));
        model.addAttribute("roles", List.of(RoleName.REFERRAL_OFFICER, RoleName.DOCTOR, RoleName.VIEWER));
        model.addAttribute("q", q);
        model.addAttribute("selectedRole", role);
        return "hospitaladmin/users/list";
    }
