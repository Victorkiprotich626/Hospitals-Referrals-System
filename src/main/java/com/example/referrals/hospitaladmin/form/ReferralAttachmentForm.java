package com.example.referrals.hospitaladmin.form;

import com.example.referrals.referral.ReferralAttachmentType;

public class ReferralAttachmentForm {

    private ReferralAttachmentType attachmentType = ReferralAttachmentType.OTHER;
    private String note;

    public ReferralAttachmentType getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(ReferralAttachmentType attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
