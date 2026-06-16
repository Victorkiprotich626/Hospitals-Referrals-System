package com.example.referrals.web;

import com.example.referrals.hospitaladmin.form.DoctorForm;
import com.example.referrals.hospitaladmin.service.HospitalAdminDirectoryService;
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
@RequestMapping("/hospital-admin/doctors")
public class HospitalAdminDoctorController {

    private final HospitalAdminDirectoryService directoryService;

    public HospitalAdminDoctorController(HospitalAdminDirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Long departmentId,
                       Model model) {
        model.addAttribute("doctors", directoryService.findDoctorsForCurrentTenant(q, departmentId));
        model.addAttribute("departments", directoryService.findEnabledDepartmentsForCurrentTenant());
        model.addAttribute("q", q);
        model.addAttribute("departmentId", departmentId);
        return "hospitaladmin/doctors/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        populateForm(model, new DoctorForm(), "Add Doctor", "/hospital-admin/doctors");
        return "hospitaladmin/doctors/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("doctorForm") DoctorForm doctorForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, doctorForm, "Add Doctor", "/hospital-admin/doctors");
            return "hospitaladmin/doctors/form";
        }
        try {
            directoryService.createDoctor(doctorForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("doctor", ex.getMessage());
            populateForm(model, doctorForm, "Add Doctor", "/hospital-admin/doctors");
            return "hospitaladmin/doctors/form";
        }
        redirectAttributes.addFlashAttribute("message", "Doctor created successfully.");
        return "redirect:/hospital-admin/doctors";
    }

    @GetMapping("/{doctorId}/edit")
    public String editForm(@PathVariable Long doctorId, Model model) {
        populateForm(model, directoryService.buildDoctorForm(doctorId), "Edit Doctor",
            "/hospital-admin/doctors/" + doctorId);
        return "hospitaladmin/doctors/form";
    }

    @PostMapping("/{doctorId}")
    public String update(@PathVariable Long doctorId,
                         @Valid @ModelAttribute("doctorForm") DoctorForm doctorForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, doctorForm, "Edit Doctor", "/hospital-admin/doctors/" + doctorId);
            return "hospitaladmin/doctors/form";
        }
        try {
            directoryService.updateDoctor(doctorId, doctorForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("doctor", ex.getMessage());
            populateForm(model, doctorForm, "Edit Doctor", "/hospital-admin/doctors/" + doctorId);
            return "hospitaladmin/doctors/form";
        }
        redirectAttributes.addFlashAttribute("message", "Doctor updated successfully.");
        return "redirect:/hospital-admin/doctors";
    }

    @PostMapping("/{doctorId}/delete")
    public String delete(@PathVariable Long doctorId, RedirectAttributes redirectAttributes) {
        try {
            directoryService.deleteDoctor(doctorId);
            redirectAttributes.addFlashAttribute("message", "Doctor deleted successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/hospital-admin/doctors";
    }

    private void populateForm(Model model, DoctorForm form, String title, String action) {
        model.addAttribute("doctorForm", form);
        model.addAttribute("departments", directoryService.findEnabledDepartmentsForCurrentTenant());
        model.addAttribute("pageTitle", title);
        model.addAttribute("formAction", action);
    }
}
