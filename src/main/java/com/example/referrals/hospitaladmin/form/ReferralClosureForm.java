package com.example.referrals.hospitaladmin.form;

import com.example.referrals.referral.ReferralClosureOutcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReferralClosureForm {

    @NotNull(message = "Select a final outcome")
    private ReferralClosureOutcome closureOutcome;

    @Size(max = 500)
    private String closureSummary;

    public ReferralClosureOutcome getClosureOutcome() {
        return closureOutcome;
    }

    public void setClosureOutcome(ReferralClosureOutcome closureOutcome) {
        this.closureOutcome = closureOutcome;
    }

    public String getClosureSummary() {
        return closureSummary;
    }

    public void setClosureSummary(String closureSummary) {
        this.closureSummary = closureSummary;
    }
}
