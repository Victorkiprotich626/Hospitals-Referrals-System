package com.example.referrals.referral;

public enum ReferralAttachmentType {
    REFERRAL_LETTER("Referral Letter"),
    LAB_RESULT("Lab Result"),
    IMAGING("Imaging"),
    PRESCRIPTION("Prescription"),
    DISCHARGE_SUMMARY("Discharge Summary"),
    CLINICAL_NOTE("Clinical Note"),
    OTHER("Other");

    private final String displayName;

    ReferralAttachmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
