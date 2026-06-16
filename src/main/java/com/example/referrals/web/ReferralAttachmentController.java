package com.example.referrals.web;

import com.example.referrals.referral.ReferralAttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/attachments")
public class ReferralAttachmentController {

    private final ReferralAttachmentService attachmentService;

    public ReferralAttachmentController(ReferralAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long attachmentId) {
        ReferralAttachmentService.DownloadedAttachment download = attachmentService.loadVisibleAttachment(attachmentId);
        return buildFileResponse(download, false);
    }

    @GetMapping("/{attachmentId}/view")
    public ResponseEntity<Resource> view(@PathVariable Long attachmentId) {
        ReferralAttachmentService.DownloadedAttachment download = attachmentService.loadVisibleAttachment(attachmentId);
        return buildFileResponse(download, true);
    }
