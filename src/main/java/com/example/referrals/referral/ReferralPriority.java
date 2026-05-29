package com.example.referrals.referral;

import java.time.Duration;

public enum ReferralPriority {
    LOW("Low", Duration.ofHours(72)),
    NORMAL("Normal", Duration.ofHours(48)),
    HIGH("High", Duration.ofHours(24)),
    URGENT("Urgent", Duration.ofHours(4));

    private final String displayName;
    private final Duration slaDuration;

    ReferralPriority(String displayName, Duration slaDuration) {
        this.displayName = displayName;
        this.slaDuration = slaDuration;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Duration getSlaDuration() {
        return slaDuration;
    }
}
