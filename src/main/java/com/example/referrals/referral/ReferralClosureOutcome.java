package com.example.referrals.referral;

public enum ReferralClosureOutcome {
    TREATED("Treated"),
    ADMITTED("Admitted"),
    REDIRECTED("Redirected"),
    CLOSED_WITHOUT_TREATMENT("Closed Without Treatment");

    private final String displayName;

    ReferralClosureOutcome(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
