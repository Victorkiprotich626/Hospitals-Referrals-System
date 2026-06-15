package com.example.referrals.web;

import com.example.referrals.hospitaladmin.form.DepartmentForm;
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
@RequestMapping("/hospital-admin/departments")
public class HospitalAdminDepartmentController {

    private final HospitalAdminDirectoryService directoryService;

    public HospitalAdminDepartmentController(HospitalAdminDirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("departments", directoryService.findDepartmentsForCurrentTenant(q));
        model.addAttribute("q", q);
        return "hospitaladmin/departments/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        populateForm(model, new DepartmentForm(), "Add Department", "/hospital-admin/departments");
        return "hospitaladmin/departments/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("departmentForm") DepartmentForm departmentForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, departmentForm, "Add Department", "/hospital-admin/departments");
            return "hospitaladmin/departments/form";
        }
        try {
            directoryService.createDepartment(departmentForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("department", ex.getMessage());
            populateForm(model, departmentForm, "Add Department", "/hospital-admin/departments");
            return "hospitaladmin/departments/form";
        }
        redirectAttributes.addFlashAttribute("message", "Department created successfully.");
        return "redirect:/hospital-admin/departments";
    }

    @GetMapping("/{departmentId}/edit")
    public String editForm(@PathVariable Long departmentId, Model model) {
        populateForm(model, directoryService.buildDepartmentForm(departmentId), "Edit Department",
            "/hospital-admin/departments/" + departmentId);
        return "hospitaladmin/departments/form";
    }
