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

    @GetMapping("/new")
    public String createForm(Model model) {
        populateForm(model, new HospitalUserForm(), "Add User", "/hospital-admin/users");
        return "hospitaladmin/users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("hospitalUserForm") HospitalUserForm hospitalUserForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, hospitalUserForm, "Add User", "/hospital-admin/users");
            return "hospitaladmin/users/form";
        }
        try {
            staffService.create(hospitalUserForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("user", ex.getMessage());
            populateForm(model, hospitalUserForm, "Add User", "/hospital-admin/users");
            return "hospitaladmin/users/form";
        }
        redirectAttributes.addFlashAttribute("message", "Hospital user created successfully.");
        return "redirect:/hospital-admin/users";
    }

    @GetMapping("/{userId}/edit")
    public String editForm(@PathVariable Long userId, Model model) {
        populateForm(model, staffService.buildForm(userId), "Edit User", "/hospital-admin/users/" + userId);
        return "hospitaladmin/users/form";
    }

    @PostMapping("/{userId}")
    public String update(@PathVariable Long userId,
                         @Valid @ModelAttribute("hospitalUserForm") HospitalUserForm hospitalUserForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, hospitalUserForm, "Edit User", "/hospital-admin/users/" + userId);
            return "hospitaladmin/users/form";
        }
        try {
            staffService.update(userId, hospitalUserForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("user", ex.getMessage());
            populateForm(model, hospitalUserForm, "Edit User", "/hospital-admin/users/" + userId);
            return "hospitaladmin/users/form";
        }
        redirectAttributes.addFlashAttribute("message", "Hospital user updated successfully.");
        return "redirect:/hospital-admin/users";
    }

    @PostMapping("/{userId}/delete")
    public String delete(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        staffService.delete(userId);
        redirectAttributes.addFlashAttribute("message", "Hospital user deleted successfully.");
        return "redirect:/hospital-admin/users";
    }

    private void populateForm(Model model, HospitalUserForm form, String title, String action) {
        model.addAttribute("hospitalUserForm", form);
        model.addAttribute("roles", List.of(RoleName.REFERRAL_OFFICER, RoleName.DOCTOR, RoleName.VIEWER));
        model.addAttribute("departments", directoryService.findEnabledDepartmentsForCurrentTenant());
        model.addAttribute("doctors", directoryService.findEnabledDoctorsForCurrentTenant());
        model.addAttribute("pageTitle", title);
        model.addAttribute("formAction", action);
    }
}
