package com.example.referrals.referral;

import com.example.referrals.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Locale;

@Entity
@Table(name = "referral_attachments")
public class ReferralAttachment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referral_id", nullable = false)
    private Referral referral;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 255)
    private String storedFileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReferralAttachmentType attachmentType;

    @Column(length = 120)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(length = 600)
    private String note;

    @Column(nullable = false, length = 160)
    private String uploadedByName;

    @Column(length = 80)
    private String uploadedByRoleName;

    public Referral getReferral() {
        return referral;
    }

    public void setReferral(Referral referral) {
        this.referral = referral;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public ReferralAttachmentType getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(ReferralAttachmentType attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getUploadedByName() {
        return uploadedByName;
    }

    public void setUploadedByName(String uploadedByName) {
        this.uploadedByName = uploadedByName;
    }

    public String getUploadedByRoleName() {
        return uploadedByRoleName;
    }

    public void setUploadedByRoleName(String uploadedByRoleName) {
        this.uploadedByRoleName = uploadedByRoleName;
    }

    public boolean hasNote() {
        return note != null && !note.isBlank();
    }

    public boolean isPreviewableInline() {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return type.equals("application/pdf")
            || type.equals("image/jpeg")
            || type.equals("image/png");
    }

    public String getDisplaySize() {
        if (fileSize < 1024) {
            return fileSize + " bytes";
        }
        if (fileSize < 1024 * 1024) {
            return Math.round(fileSize / 1024.0) + " KB";
        }
        return String.format(Locale.ROOT, "%.1f MB", fileSize / (1024.0 * 1024.0));
    }
}
