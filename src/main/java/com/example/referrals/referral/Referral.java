package com.example.referrals.referral;

import com.example.referrals.common.model.AuditableEntity;
import com.example.referrals.directory.Department;
import com.example.referrals.directory.Doctor;
import com.example.referrals.hospital.Hospital;
import com.example.referrals.patient.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "referrals")
public class Referral extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String referenceNumber;

    @Column(nullable = false, length = 60)
    private String journeyCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_referral_id")
    private Referral parentReferral;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_hospital_id", nullable = false)
    private Hospital fromHospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_hospital_id", nullable = false)
    private Hospital toHospital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReferralStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferralPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private ReferralClosureOutcome closureOutcome;

    @Column(nullable = false, length = 180)
    private String subject;

    @Column(nullable = false, length = 1200)
    private String referralReason;

    @Column(length = 2500)
    private String clinicalSummary;

    @Column(length = 500)
    private String closureSummary;

    @Column(length = 150)
    private String receivingDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_department_id")
    private Department assignedDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_doctor_id")
    private Doctor assignedDoctor;

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getJourneyCode() {
        return journeyCode;
    }

    public void setJourneyCode(String journeyCode) {
        this.journeyCode = journeyCode;
    }

    public Referral getParentReferral() {
        return parentReferral;
    }

    public void setParentReferral(Referral parentReferral) {
        this.parentReferral = parentReferral;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Hospital getFromHospital() {
        return fromHospital;
    }

    public void setFromHospital(Hospital fromHospital) {
        this.fromHospital = fromHospital;
    }

    public Hospital getToHospital() {
        return toHospital;
    }

    public void setToHospital(Hospital toHospital) {
        this.toHospital = toHospital;
    }

    public ReferralStatus getStatus() {
        return status;
    }

    public void setStatus(ReferralStatus status) {
        this.status = status;
    }

    public ReferralPriority getPriority() {
        return priority;
    }

    public void setPriority(ReferralPriority priority) {
        this.priority = priority;
    }

    public ReferralClosureOutcome getClosureOutcome() {
        return closureOutcome;
    }

    public void setClosureOutcome(ReferralClosureOutcome closureOutcome) {
        this.closureOutcome = closureOutcome;
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

    public String getClosureSummary() {
        return closureSummary;
    }

    public void setClosureSummary(String closureSummary) {
        this.closureSummary = closureSummary;
    }

    public String getReceivingDepartment() {
        return receivingDepartment;
    }

    public void setReceivingDepartment(String receivingDepartment) {
        this.receivingDepartment = receivingDepartment;
    }

    public Department getAssignedDepartment() {
        return assignedDepartment;
    }

    public void setAssignedDepartment(Department assignedDepartment) {
        this.assignedDepartment = assignedDepartment;
    }

    public Doctor getAssignedDoctor() {
        return assignedDoctor;
    }

    public void setAssignedDoctor(Doctor assignedDoctor) {
        this.assignedDoctor = assignedDoctor;
    }

    public boolean isIncomingFor(Long hospitalId) {
        return hospitalId != null && toHospital != null && hospitalId.equals(toHospital.getId());
    }

    public boolean isOutgoingFor(Long hospitalId) {
        return hospitalId != null && fromHospital != null && hospitalId.equals(fromHospital.getId());
    }

    public boolean isClosed() {
        return status != null && status.isClosed();
    }
