package com.example.referrals.notification;

public enum NotificationType {
    REFERRAL_CREATED("Referral Created"),
    REFERRAL_ASSIGNED("Referral Assigned"),
    REFERRAL_STATUS_CHANGED("Referral Status Changed"),
    REFERRAL_NOTE_ADDED("Referral Note Added"),
    REFERRAL_ATTACHMENT_ADDED("Referral Attachment Added");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
