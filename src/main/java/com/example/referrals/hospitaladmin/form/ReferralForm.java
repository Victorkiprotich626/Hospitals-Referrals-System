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

    public Long getSourceReferralId() {
        return sourceReferralId;
    }

    public void setSourceReferralId(Long sourceReferralId) {
        this.sourceReferralId = sourceReferralId;
    }

    public Long getToHospitalId() {
        return toHospitalId;
    }

    public void setToHospitalId(Long toHospitalId) {
        this.toHospitalId = toHospitalId;
    }

    public ReferralPriority getPriority() {
        return priority;
    }

    public void setPriority(ReferralPriority priority) {
        this.priority = priority;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getReferralReason() {
        return referralReason;
    }

    public void setReferralReason(String referralReason) {
        this.referralReason = referralReason;
    }

    public String getClinicalSummary() {
        return clinicalSummary;
    }

    public void setClinicalSummary(String clinicalSummary) {
        this.clinicalSummary = clinicalSummary;
    }

    public String getReceivingDepartment() {
        return receivingDepartment;
    }

    public void setReceivingDepartment(String receivingDepartment) {
        this.receivingDepartment = receivingDepartment;
    }
}
