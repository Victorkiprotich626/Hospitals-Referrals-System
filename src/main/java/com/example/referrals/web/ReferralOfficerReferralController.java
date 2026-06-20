package com.example.referrals.web;

import com.example.referrals.hospitaladmin.form.ReferralAssignmentForm;
import com.example.referrals.hospitaladmin.form.ReferralAttachmentForm;
import com.example.referrals.hospitaladmin.form.ReferralClosureForm;
import com.example.referrals.hospitaladmin.form.ReferralForm;
import com.example.referrals.hospitaladmin.form.ReferralNoteForm;
import com.example.referrals.hospitaladmin.form.ReferralStatusForm;
import com.example.referrals.hospitaladmin.service.HospitalAdminReferralService;
import com.example.referrals.referral.ReferralAttachmentService;
import com.example.referrals.referral.Referral;
import com.example.referrals.referral.ReferralClosureOutcome;
import com.example.referrals.referral.ReferralEventType;
import com.example.referrals.referral.ReferralPriority;
import com.example.referrals.referral.ReferralStatus;
import com.example.referrals.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/referral-officer/referrals")
public class ReferralOfficerReferralController {

    private final HospitalAdminReferralService referralService;
    private final ReferralAttachmentService attachmentService;

    public ReferralOfficerReferralController(HospitalAdminReferralService referralService,
                                             ReferralAttachmentService attachmentService) {
        this.referralService = referralService;
        this.attachmentService = attachmentService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) ReferralStatus status,
                       @RequestParam(required = false) String direction,
                       Model model) {
        model.addAttribute("referrals", referralService.findAllVisibleToCurrentTenant(q, status, direction));
        model.addAttribute("currentHospitalId", TenantContext.getTenantId());
        model.addAttribute("q", q);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedDirection", direction);
        model.addAttribute("statuses", ReferralStatus.values());
        return "referralofficer/referrals/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        populateCreateForm(model, new ReferralForm());
        return "referralofficer/referrals/form";
    }

    @GetMapping("/{referralId}")
    public String detail(@PathVariable Long referralId,
                         @RequestParam(required = false) String timelineScope,
                         @RequestParam(required = false) String eventType,
                         Model model) {
        Referral referral = referralService.findVisibleById(referralId);
        populateDetailModel(model, referral, referralId, timelineScope, parseEventType(eventType));
        return "referralofficer/referrals/detail";
    }

    @GetMapping("/{referralId}/forward")
    public String forwardForm(@PathVariable Long referralId, Model model) {
        Referral sourceReferral = referralService.findVisibleById(referralId);
        populateForwardForm(model, referralService.buildForwardForm(referralId), sourceReferral);
        return "referralofficer/referrals/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("referralForm") ReferralForm referralForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            repopulateForm(model, referralForm);
            return "referralofficer/referrals/form";
        }

        Referral referral;
        try {
            referral = referralService.create(referralForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("referral", ex.getMessage());
            repopulateForm(model, referralForm);
            return "referralofficer/referrals/form";
        }

        String action = referralForm.getSourceReferralId() == null ? "created" : "forwarded";
        redirectAttributes.addFlashAttribute("message",
            "Referral " + referral.getReferenceNumber() + " " + action + " successfully.");
        return "redirect:/referral-officer/referrals/" + referral.getId();
    }

    @PostMapping("/{referralId}/attachments")
    public String uploadAttachment(@PathVariable Long referralId,
                                   @ModelAttribute("attachmentForm") ReferralAttachmentForm attachmentForm,
                                   @RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        try {
            attachmentService.uploadVisibleAttachment(referralId, attachmentForm.getAttachmentType(), attachmentForm.getNote(), file);
            redirectAttributes.addFlashAttribute("message", "Attachment uploaded successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/referral-officer/referrals/" + referralId;
    }

    @PostMapping("/{referralId}/attachments/{attachmentId}/delete")
    public String deleteAttachment(@PathVariable Long referralId,
                                   @PathVariable Long attachmentId,
                                   RedirectAttributes redirectAttributes) {
        try {
            attachmentService.deleteVisibleAttachment(referralId, attachmentId);
            redirectAttributes.addFlashAttribute("message", "Attachment deleted successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/referral-officer/referrals/" + referralId;
    }

    @PostMapping("/{referralId}/assign")
    public String assign(@PathVariable Long referralId,
                         @ModelAttribute("assignmentForm") ReferralAssignmentForm assignmentForm,
                         RedirectAttributes redirectAttributes) {
        try {
            referralService.assign(referralId, assignmentForm);
            redirectAttributes.addFlashAttribute("message", "Referral routed successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/referral-officer/referrals/" + referralId;
    }

    @PostMapping("/{referralId}/status")
    public String updateStatus(@PathVariable Long referralId,
                               @Valid @ModelAttribute("statusForm") ReferralStatusForm statusForm,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Referral referral = referralService.findVisibleById(referralId);
            populateDetailModel(model, referral, referralId, null, null);
            model.addAttribute("noteForm", new ReferralNoteForm());
            return "referralofficer/referrals/detail";
        }
        try {
            referralService.updateStatus(referralId, statusForm);
            redirectAttributes.addFlashAttribute("message", "Referral status updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/referral-officer/referrals/" + referralId;
    }

    @PostMapping("/{referralId}/notes")
    public String addNote(@PathVariable Long referralId,
                          @Valid @ModelAttribute("noteForm") ReferralNoteForm noteForm,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Referral referral = referralService.findVisibleById(referralId);
            populateDetailModel(model, referral, referralId, null, null);
            model.addAttribute("statusForm", new ReferralStatusForm());
            return "referralofficer/referrals/detail";
        }
        referralService.addNote(referralId, noteForm);
        redirectAttributes.addFlashAttribute("message", "Timeline note added successfully.");
        return "redirect:/referral-officer/referrals/" + referralId;
    }

    @PostMapping("/{referralId}/closure")
    public String updateClosureOutcome(@PathVariable Long referralId,
                                       @Valid @ModelAttribute("closureForm") ReferralClosureForm closureForm,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Referral referral = referralService.findVisibleById(referralId);
            populateDetailModel(model, referral, referralId, null, null);
            model.addAttribute("statusForm", new ReferralStatusForm());
            model.addAttribute("closureForm", closureForm);
            return "referralofficer/referrals/detail";
        }
        try {
            referralService.updateClosureOutcome(referralId, closureForm);
            redirectAttributes.addFlashAttribute("message", "Final outcome updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/referral-officer/referrals/" + referralId;
    }

    private void populateCreateForm(Model model, ReferralForm form) {
        model.addAttribute("referralForm", form);
        model.addAttribute("patients", referralService.findPatientsForCurrentTenant());
        model.addAttribute("hospitals", referralService.findReferralDestinations());
        model.addAttribute("priorities", ReferralPriority.values());
        model.addAttribute("pageTitle", "Create Referral");
        model.addAttribute("formSubtitle", "Submit a new referral from your hospital to another hospital in the platform.");
        model.addAttribute("sourceReferral", null);
        model.addAttribute("formAction", "/referral-officer/referrals");
        model.addAttribute("submitLabel", "Submit referral");
    }

    private void populateForwardForm(Model model, ReferralForm form, Referral sourceReferral) {
        model.addAttribute("referralForm", form);
        model.addAttribute("patients", referralService.findPatientsForCurrentTenant());
        model.addAttribute("hospitals", referralService.findReferralDestinations());
        model.addAttribute("priorities", ReferralPriority.values());
        model.addAttribute("pageTitle", "Forward Referral");
        model.addAttribute("formSubtitle", "Create the next referral in this patient's journey while preserving cross-hospital tracking.");
        model.addAttribute("sourceReferral", sourceReferral);
        model.addAttribute("formAction", "/referral-officer/referrals");
        model.addAttribute("submitLabel", "Forward referral");
    }

    private void repopulateForm(Model model, ReferralForm form) {
        if (form.getSourceReferralId() == null) {
            populateCreateForm(model, form);
            return;
        }
        populateForwardForm(model, form, referralService.findVisibleById(form.getSourceReferralId()));
    }

    private void populateDetailModel(Model model,
                                     Referral referral,
                                     Long referralId,
                                     String timelineScope,
                                     ReferralEventType eventType) {
        String resolvedScope = StringUtils.hasText(timelineScope) ? timelineScope.trim().toLowerCase() : "referral";
        boolean journeyScope = "journey".equals(resolvedScope);
        model.addAttribute("referral", referral);
        model.addAttribute("timeline", journeyScope
            ? referralService.findJourneyTimelineForCurrentTenant(referralId, eventType)
            : referralService.findTimeline(referralId, eventType));
        model.addAttribute("timelineScope", journeyScope ? "journey" : "referral");
        model.addAttribute("selectedTimelineEventType", eventType != null ? eventType.name() : "");
        model.addAttribute("timelineEventTypes", ReferralEventType.values());
        model.addAttribute("allowedStatuses", referralService.allowedTransitions(referral));
        model.addAttribute("departments", referralService.findEnabledDepartmentsForCurrentTenant());
        model.addAttribute("doctors", referralService.findEnabledDoctorsForCurrentTenant());
        model.addAttribute("attachments", attachmentService.findVisibleAttachments(referralId));
        model.addAttribute("attachmentForm", new ReferralAttachmentForm());
        model.addAttribute("attachmentTypes", com.example.referrals.referral.ReferralAttachmentType.values());
        model.addAttribute("canManageAttachments", attachmentService.canManageAttachments());
        model.addAttribute("journeyReferrals", referralService.findJourneyForCurrentTenant(referral.getJourneyCode()));
        model.addAttribute("canForward", referralService.canForward(referral));
        model.addAttribute("assignmentForm", new ReferralAssignmentForm());
        model.addAttribute("statusForm", new ReferralStatusForm());
        model.addAttribute("closureForm", referralService.buildClosureForm(referral));
        model.addAttribute("closureOutcomes", ReferralClosureOutcome.values());
        model.addAttribute("noteForm", new ReferralNoteForm());
        model.addAttribute("currentHospitalId", TenantContext.getTenantId());
    }

    private ReferralEventType parseEventType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return null;
        }
        try {
            return ReferralEventType.valueOf(eventType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
