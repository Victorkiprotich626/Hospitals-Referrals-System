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

    public String getActorHospitalName() {
        return actorHospitalName;
    }

    public void setActorHospitalName(String actorHospitalName) {
        this.actorHospitalName = actorHospitalName;
    }

    public String getActorRoleName() {
        return actorRoleName;
    }

    public void setActorRoleName(String actorRoleName) {
        this.actorRoleName = actorRoleName;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getTransitionSummary() {
        if (previousStatus == null || newStatus == null) {
            return null;
        }
        return previousStatus.getDisplayName() + " -> " + newStatus.getDisplayName();
    }

    public boolean isMilestone() {
        return eventType == ReferralEventType.CREATED
            || eventType == ReferralEventType.STATUS_CHANGED
            || eventType == ReferralEventType.OUTCOME_UPDATED
            || eventType == ReferralEventType.ASSIGNED
            || eventType == ReferralEventType.ATTACHMENT_REMOVED;
    }

    public String getActorSummary() {
        StringBuilder summary = new StringBuilder(actorName == null ? "System" : actorName);
        if (actorRoleName != null && !actorRoleName.isBlank()) {
            summary.append(" (").append(actorRoleName);
            if (actorHospitalName != null && !actorHospitalName.isBlank()) {
                summary.append(", ").append(actorHospitalName);
            }
            summary.append(')');
            return summary.toString();
        }
        if (actorHospitalName != null && !actorHospitalName.isBlank()) {
            summary.append(" • ").append(actorHospitalName);
        }
        return summary.toString();
    }

    public String getRelativeTimeLabel() {
        LocalDateTime createdAt = getCreatedAt();
        if (createdAt == null) {
            return "";
        }

        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long minutes = Math.max(duration.toMinutes(), 0);
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }

        long hours = duration.toHours();
        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }

        long days = duration.toDays();
        if (days < 30) {
            return days + (days == 1 ? " day ago" : " days ago");
        }

        long months = Math.max(days / 30, 1);
        if (months < 12) {
            return months + (months == 1 ? " month ago" : " months ago");
        }

        long years = Math.max(days / 365, 1);
        return years + (years == 1 ? " year ago" : " years ago");
    }
}
