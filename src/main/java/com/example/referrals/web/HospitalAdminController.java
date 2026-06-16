package com.example.referrals.web;

import com.example.referrals.superadmin.form.HospitalAdminForm;
import com.example.referrals.superadmin.service.SuperAdminUserService;
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

@Controller
@RequestMapping("/super-admin/admins")
public class HospitalAdminController {

    private final SuperAdminUserService userService;

    public HospitalAdminController(SuperAdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("admins", userService.findHospitalAdmins(q));
        model.addAttribute("q", q);
        return "superadmin/admins/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        populateForm(model, new HospitalAdminForm(), "Add Hospital Admin", "/super-admin/admins");
        return "superadmin/admins/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("hospitalAdminForm") HospitalAdminForm hospitalAdminForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, hospitalAdminForm, "Add Hospital Admin", "/super-admin/admins");
            return "superadmin/admins/form";
        }

        try {
            userService.createHospitalAdmin(hospitalAdminForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("admin", ex.getMessage());
            populateForm(model, hospitalAdminForm, "Add Hospital Admin", "/super-admin/admins");
            return "superadmin/admins/form";
        }

        redirectAttributes.addFlashAttribute("message", "Hospital admin created successfully.");
        return "redirect:/super-admin/admins";
    }

    @GetMapping("/{userId}/edit")
    public String editForm(@PathVariable Long userId, Model model) {
        populateForm(model, userService.buildForm(userId), "Edit Hospital Admin", "/super-admin/admins/" + userId);
        return "superadmin/admins/form";
    }

    @PostMapping("/{userId}")
    public String update(@PathVariable Long userId,
                         @Valid @ModelAttribute("hospitalAdminForm") HospitalAdminForm hospitalAdminForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, hospitalAdminForm, "Edit Hospital Admin", "/super-admin/admins/" + userId);
            return "superadmin/admins/form";
        }

        try {
            userService.updateHospitalAdmin(userId, hospitalAdminForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("admin", ex.getMessage());
            populateForm(model, hospitalAdminForm, "Edit Hospital Admin", "/super-admin/admins/" + userId);
            return "superadmin/admins/form";
        }

        redirectAttributes.addFlashAttribute("message", "Hospital admin updated successfully.");
        return "redirect:/super-admin/admins";
    }

    @PostMapping("/{userId}/delete")
    public String delete(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        userService.deleteHospitalAdmin(userId);
        redirectAttributes.addFlashAttribute("message", "Hospital admin deleted successfully.");
        return "redirect:/super-admin/admins";
    }

    private void populateForm(Model model, HospitalAdminForm form, String title, String action) {
        model.addAttribute("hospitalAdminForm", form);
        model.addAttribute("hospitals", userService.findHospitalsForSelection());
        model.addAttribute("pageTitle", title);
        model.addAttribute("formAction", action);
    }
}
