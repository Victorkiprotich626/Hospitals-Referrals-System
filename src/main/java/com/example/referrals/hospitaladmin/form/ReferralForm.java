package com.example.referrals.hospitaladmin.form;

import com.example.referrals.referral.ReferralPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReferralForm {

    private Long patientId;

    private Long sourceReferralId;

    @NotNull(message = "Select the receiving hospital")
    private Long toHospitalId;

    @NotNull(message = "Select a priority")
    private ReferralPriority priority;

    @NotBlank(message = "Referral subject is required")
    @Size(max = 180)
    private String subject;

    @NotBlank(message = "Referral reason is required")
    @Size(max = 1200)
    private String referralReason;

    @Size(max = 2500)
    private String clinicalSummary;

    @Size(max = 150)
    private String receivingDepartment;

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }
