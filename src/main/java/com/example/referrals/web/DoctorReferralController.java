package com.example.referrals.web;

import com.example.referrals.hospitaladmin.form.ReferralAttachmentForm;
import com.example.referrals.hospitaladmin.form.ReferralNoteForm;
import com.example.referrals.hospitaladmin.service.HospitalAdminReferralService;
import com.example.referrals.referral.ReferralAttachmentService;
import com.example.referrals.referral.Referral;
import com.example.referrals.referral.ReferralEventType;
import com.example.referrals.referral.ReferralStatus;
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
@RequestMapping("/doctor/referrals")
public class DoctorReferralController {

    private final HospitalAdminReferralService referralService;
    private final ReferralAttachmentService attachmentService;

    public DoctorReferralController(HospitalAdminReferralService referralService,
                                    ReferralAttachmentService attachmentService) {
        this.referralService = referralService;
        this.attachmentService = attachmentService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) ReferralStatus status,
                       Model model) {
        model.addAttribute("referrals", referralService.findAssignedToCurrentDoctor(q, status));
        model.addAttribute("q", q);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", ReferralStatus.values());
        return "doctor/referrals/list";
    }

    @GetMapping("/{referralId}")
    public String detail(@PathVariable Long referralId,
                         @RequestParam(required = false) String timelineScope,
                         @RequestParam(required = false) String eventType,
                         Model model) {
        Referral referral = referralService.findAssignedReferralForCurrentDoctor(referralId);
        populateDetailModel(model, referral, referralId, timelineScope, parseEventType(eventType));
        return "doctor/referrals/detail";
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
        return "redirect:/doctor/referrals/" + referralId;
    }

    @PostMapping("/{referralId}/notes")
    public String addNote(@PathVariable Long referralId,
                          @Valid @ModelAttribute("noteForm") ReferralNoteForm noteForm,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Referral referral = referralService.findAssignedReferralForCurrentDoctor(referralId);
            populateDetailModel(model, referral, referralId, null, null);
            return "doctor/referrals/detail";
        }
        referralService.addDoctorNote(referralId, noteForm);
        redirectAttributes.addFlashAttribute("message", "Clinical note added successfully.");
        return "redirect:/doctor/referrals/" + referralId;
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
        model.addAttribute("attachments", attachmentService.findVisibleAttachments(referralId));
        model.addAttribute("attachmentForm", new ReferralAttachmentForm());
        model.addAttribute("attachmentTypes", com.example.referrals.referral.ReferralAttachmentType.values());
        model.addAttribute("canManageAttachments", attachmentService.canManageAttachments());
        model.addAttribute("journeyReferrals", referralService.findJourneyForCurrentTenant(referral.getJourneyCode()));
        model.addAttribute("noteForm", new ReferralNoteForm());
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
