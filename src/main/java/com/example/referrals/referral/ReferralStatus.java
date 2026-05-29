package com.example.referrals.referral;

public enum ReferralStatus {
    SUBMITTED("Submitted"),
    RECEIVED("Received"),
    UNDER_REVIEW("Under Review"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    ReferralStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isClosed() {
        return this == REJECTED || this == COMPLETED || this == CANCELLED;
    }
}
