package com.example.referrals.web;

import com.example.referrals.hospitaladmin.form.PatientForm;
import com.example.referrals.hospitaladmin.service.HospitalAdminPatientService;
import com.example.referrals.patient.Gender;
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
@RequestMapping("/hospital-admin/patients")
public class HospitalAdminPatientController {

    private final HospitalAdminPatientService patientService;

    public HospitalAdminPatientController(HospitalAdminPatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("patients", patientService.findAllForCurrentTenant(q));
        model.addAttribute("q", q);
        return "hospitaladmin/patients/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        populateForm(model, new PatientForm(), "Add Patient", "/hospital-admin/patients");
        return "hospitaladmin/patients/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("patientForm") PatientForm patientForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, patientForm, "Add Patient", "/hospital-admin/patients");
            return "hospitaladmin/patients/form";
        }

        try {
            patientService.create(patientForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("patient", ex.getMessage());
            populateForm(model, patientForm, "Add Patient", "/hospital-admin/patients");
            return "hospitaladmin/patients/form";
        }

        redirectAttributes.addFlashAttribute("message", "Patient created successfully.");
        return "redirect:/hospital-admin/patients";
    }

    @GetMapping("/{patientId}/edit")
    public String editForm(@PathVariable Long patientId, Model model) {
        populateForm(model, patientService.buildForm(patientId), "Edit Patient",
            "/hospital-admin/patients/" + patientId);
        return "hospitaladmin/patients/form";
    }

    @PostMapping("/{patientId}")
    public String update(@PathVariable Long patientId,
                         @Valid @ModelAttribute("patientForm") PatientForm patientForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, patientForm, "Edit Patient", "/hospital-admin/patients/" + patientId);
            return "hospitaladmin/patients/form";
        }

        try {
            patientService.update(patientId, patientForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("patient", ex.getMessage());
            populateForm(model, patientForm, "Edit Patient", "/hospital-admin/patients/" + patientId);
            return "hospitaladmin/patients/form";
        }

        redirectAttributes.addFlashAttribute("message", "Patient updated successfully.");
        return "redirect:/hospital-admin/patients";
    }

    @PostMapping("/{patientId}/delete")
    public String delete(@PathVariable Long patientId, RedirectAttributes redirectAttributes) {
        try {
            patientService.delete(patientId);
            redirectAttributes.addFlashAttribute("message", "Patient deleted successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/hospital-admin/patients";
    }

    private void populateForm(Model model, PatientForm form, String title, String action) {
        model.addAttribute("patientForm", form);
        model.addAttribute("genders", Gender.values());
        model.addAttribute("pageTitle", title);
        model.addAttribute("formAction", action);
    }
}
