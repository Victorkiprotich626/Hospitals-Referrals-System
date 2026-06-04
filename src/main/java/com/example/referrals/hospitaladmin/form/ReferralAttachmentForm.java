package com.example.referrals.hospitaladmin.form;

import com.example.referrals.referral.ReferralAttachmentType;

public class ReferralAttachmentForm {

    private ReferralAttachmentType attachmentType = ReferralAttachmentType.OTHER;
    private String note;

    public ReferralAttachmentType getAttachmentType() {
        return attachmentType;
    }
