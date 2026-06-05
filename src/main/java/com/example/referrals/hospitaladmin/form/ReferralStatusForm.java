package com.example.referrals.hospitaladmin.form;

import com.example.referrals.referral.ReferralClosureOutcome;
import com.example.referrals.referral.ReferralStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReferralStatusForm {

    @NotNull(message = "Select the next status")
    private ReferralStatus status;

    @Size(max = 500)
    private String note;

    private ReferralClosureOutcome closureOutcome;

    @Size(max = 500)
    private String closureSummary;

    public ReferralStatus getStatus() {
        return status;
    }

    public void setStatus(ReferralStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
