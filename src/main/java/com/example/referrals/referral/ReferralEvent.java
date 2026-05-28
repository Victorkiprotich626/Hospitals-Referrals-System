package com.example.referrals.referral;

import com.example.referrals.common.model.AuditableEntity;
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
@Table(name = "referral_events")
public class ReferralEvent extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referral_id", nullable = false)
    private Referral referral;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReferralEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private ReferralStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private ReferralStatus newStatus;

    @Column(nullable = false, length = 160)
    private String actorName;

    @Column(length = 160)
    private String actorHospitalName;

    @Column(length = 80)
    private String actorRoleName;

    @Column(nullable = false, length = 2000)
    private String details;

    public Referral getReferral() {
        return referral;
    }

    public void setReferral(Referral referral) {
        this.referral = referral;
    }

    public ReferralEventType getEventType() {
        return eventType;
    }

    public void setEventType(ReferralEventType eventType) {
        this.eventType = eventType;
    }

    public ReferralStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(ReferralStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public ReferralStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(ReferralStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }
