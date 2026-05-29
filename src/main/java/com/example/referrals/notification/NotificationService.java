package com.example.referrals.notification;

import com.example.referrals.common.model.RoleName;
import com.example.referrals.common.web.CurrentUserFacade;
import com.example.referrals.referral.Referral;
import com.example.referrals.referral.ReferralStatus;
import com.example.referrals.security.CustomUserDetails;
import com.example.referrals.user.AppUser;
import com.example.referrals.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
@Transactional
public class NotificationService {

    private static final Set<RoleName> OPERATIONAL_ROLES = Set.of(RoleName.HOSPITAL_ADMIN, RoleName.REFERRAL_OFFICER);

    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserFacade currentUserFacade;

    public NotificationService(UserNotificationRepository notificationRepository,
                               UserRepository userRepository,
                               CurrentUserFacade currentUserFacade) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.currentUserFacade = currentUserFacade;
    }

    @Transactional(readOnly = true)
    public List<UserNotification> findCurrentUserNotifications() {
        return notificationRepository.findAllByRecipientUserId(requireCurrentUserId());
    }

    @Transactional(readOnly = true)
    public List<UserNotification> findCurrentUserNotifications(String query, boolean unreadOnly) {
        String normalizedQuery = normalizeQuery(query);
        return notificationRepository.findAllByRecipientUserId(requireCurrentUserId()).stream()
            .filter(notification -> !unreadOnly || notification.isUnread())
            .filter(notification -> normalizedQuery == null || matchesNotification(notification, normalizedQuery))
            .toList();
    }

    @Transactional(readOnly = true)
    public long countUnreadForCurrentUser() {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(requireCurrentUserId());
    }

    public String openNotification(Long notificationId) {
        UserNotification notification = notificationRepository.findVisibleById(notificationId, requireCurrentUserId())
            .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        markRead(notification);
        return notification.getLink() != null && !notification.getLink().isBlank()
            ? notification.getLink()
            : "/notifications";
    }

    public void markAllCurrentUserNotificationsRead() {
        List<UserNotification> notifications = notificationRepository.findAllByRecipientIdAndReadAtIsNull(requireCurrentUserId());
        notifications.forEach(this::markRead);
    }

    public void notifyReferralCreated(Referral referral) {
        createNotifications(
            referral,
            NotificationType.REFERRAL_CREATED,
            "New referral received",
            referral.getReferenceNumber() + " was submitted by " + referral.getFromHospital().getName() + ".",
            recipientsForHospital(referral.getToHospital().getId()),
            user -> buildReferralLink(user, referral)
        );
    }

    public void notifyReferralAssigned(Referral referral) {
        Map<Long, AppUser> recipients = new LinkedHashMap<>();
        recipientsForHospital(referral.getToHospital().getId()).forEach(user -> recipients.put(user.getId(), user));
        recipientsForAssignedDoctor(referral).forEach(user -> recipients.put(user.getId(), user));

        createNotifications(
            referral,
            NotificationType.REFERRAL_ASSIGNED,
            "Referral routed internally",
            referral.getReferenceNumber() + " was routed"
                + (referral.getAssignedDoctor() != null ? " to Dr. " + referral.getAssignedDoctor().getFullName() : "")
                + (referral.getAssignedDepartment() != null ? " in " + referral.getAssignedDepartment().getName() : "") + ".",
            recipients.values().stream().toList(),
            user -> buildReferralLink(user, referral)
        );
    }

    public void notifyReferralStatusChanged(Referral referral, ReferralStatus previousStatus, ReferralStatus newStatus) {
        Map<Long, AppUser> recipients = new LinkedHashMap<>();
        recipientsForHospital(referral.getFromHospital().getId()).forEach(user -> recipients.put(user.getId(), user));
        recipientsForHospital(referral.getToHospital().getId()).forEach(user -> recipients.put(user.getId(), user));
        recipientsForAssignedDoctor(referral).forEach(user -> recipients.put(user.getId(), user));

        createNotifications(
            referral,
            NotificationType.REFERRAL_STATUS_CHANGED,
            "Referral status changed",
            referral.getReferenceNumber() + " moved from " + previousStatus.getDisplayName()
                + " to " + newStatus.getDisplayName() + ".",
            recipients.values().stream().toList(),
            user -> buildReferralLink(user, referral)
        );
    }

    public void notifyReferralNoteAdded(Referral referral) {
        Map<Long, AppUser> recipients = new LinkedHashMap<>();
        recipientsForHospital(referral.getFromHospital().getId()).forEach(user -> recipients.put(user.getId(), user));
        recipientsForHospital(referral.getToHospital().getId()).forEach(user -> recipients.put(user.getId(), user));
        recipientsForAssignedDoctor(referral).forEach(user -> recipients.put(user.getId(), user));

        createNotifications(
            referral,
            NotificationType.REFERRAL_NOTE_ADDED,
            "Referral note added",
            "A new note was added to " + referral.getReferenceNumber() + ".",
            recipients.values().stream().toList(),
            user -> buildReferralLink(user, referral)
        );
    }
