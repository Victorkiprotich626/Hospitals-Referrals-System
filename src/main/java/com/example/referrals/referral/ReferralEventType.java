package com.example.referrals.referral;

public enum ReferralEventType {
    CREATED("Created"),
    ASSIGNED("Assigned"),
    ATTACHMENT_ADDED("Attachment Added"),
    ATTACHMENT_REMOVED("Attachment Removed"),
    STATUS_CHANGED("Status Changed"),
    OUTCOME_UPDATED("Outcome Updated"),
    NOTE_ADDED("Note Added");

    private final String displayName;

    ReferralEventType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
