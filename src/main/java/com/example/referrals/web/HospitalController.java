package com.example.referrals.web;

import com.example.referrals.superadmin.form.HospitalForm;
import com.example.referrals.superadmin.service.SuperAdminHospitalService;
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
@RequestMapping("/super-admin/hospitals")
public class HospitalController {

    private final SuperAdminHospitalService hospitalService;

    public HospitalController(SuperAdminHospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("hospitals", hospitalService.findAll(q));
        model.addAttribute("q", q);
        return "superadmin/hospitals/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("hospitalForm", new HospitalForm());
        model.addAttribute("pageTitle", "Add Hospital");
        model.addAttribute("formAction", "/super-admin/hospitals");
        return "superadmin/hospitals/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("hospitalForm") HospitalForm hospitalForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Add Hospital");
            model.addAttribute("formAction", "/super-admin/hospitals");
            return "superadmin/hospitals/form";
        }

        try {
            hospitalService.create(hospitalForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("hospital", ex.getMessage());
            model.addAttribute("pageTitle", "Add Hospital");
            model.addAttribute("formAction", "/super-admin/hospitals");
            return "superadmin/hospitals/form";
        }

        redirectAttributes.addFlashAttribute("message", "Hospital created successfully.");
        return "redirect:/super-admin/hospitals";
    }

    @GetMapping("/{hospitalId}/edit")
    public String editForm(@PathVariable Long hospitalId, Model model) {
        model.addAttribute("hospitalForm", hospitalService.buildForm(hospitalId));
        model.addAttribute("pageTitle", "Edit Hospital");
        model.addAttribute("formAction", "/super-admin/hospitals/" + hospitalId);
        return "superadmin/hospitals/form";
    }

    @PostMapping("/{hospitalId}")
    public String update(@PathVariable Long hospitalId,
                         @Valid @ModelAttribute("hospitalForm") HospitalForm hospitalForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Edit Hospital");
            model.addAttribute("formAction", "/super-admin/hospitals/" + hospitalId);
            return "superadmin/hospitals/form";
        }

        try {
            hospitalService.update(hospitalId, hospitalForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("hospital", ex.getMessage());
            model.addAttribute("pageTitle", "Edit Hospital");
            model.addAttribute("formAction", "/super-admin/hospitals/" + hospitalId);
            return "superadmin/hospitals/form";
        }

        redirectAttributes.addFlashAttribute("message", "Hospital updated successfully.");
        return "redirect:/super-admin/hospitals";
    }

    @PostMapping("/{hospitalId}/delete")
    public String delete(@PathVariable Long hospitalId, RedirectAttributes redirectAttributes) {
        try {
            hospitalService.delete(hospitalId);
            redirectAttributes.addFlashAttribute("message", "Hospital deleted successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/super-admin/hospitals";
    }
}
