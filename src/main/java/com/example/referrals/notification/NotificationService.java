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

    public void notifyReferralAttachmentAdded(Referral referral, String fileName) {
        Map<Long, AppUser> recipients = new LinkedHashMap<>();
        recipientsForHospital(referral.getFromHospital().getId()).forEach(user -> recipients.put(user.getId(), user));
        recipientsForHospital(referral.getToHospital().getId()).forEach(user -> recipients.put(user.getId(), user));
        recipientsForAssignedDoctor(referral).forEach(user -> recipients.put(user.getId(), user));

        createNotifications(
            referral,
            NotificationType.REFERRAL_ATTACHMENT_ADDED,
            "Referral attachment uploaded",
            fileName + " was uploaded to " + referral.getReferenceNumber() + ".",
            recipients.values().stream().toList(),
            user -> buildReferralLink(user, referral)
        );
    }

    private void createNotifications(Referral referral,
                                     NotificationType type,
                                     String title,
                                     String message,
                                     List<AppUser> recipients,
                                     Function<AppUser, String> linkBuilder) {
        Long currentUserId = requireCurrentUserId();
        for (AppUser recipient : recipients) {
            if (!recipient.isEnabled() || recipient.getId().equals(currentUserId)) {
                continue;
            }
            UserNotification notification = new UserNotification();
            notification.setRecipient(recipient);
            notification.setReferral(referral);
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setLink(linkBuilder.apply(recipient));
            notificationRepository.save(notification);
        }
    }

    private List<AppUser> recipientsForHospital(Long hospitalId) {
        return userRepository.findAllByHospitalIdAndRoles(hospitalId, OPERATIONAL_ROLES);
    }

    private List<AppUser> recipientsForAssignedDoctor(Referral referral) {
        if (referral.getAssignedDoctor() == null) {
            return List.of();
        }
        return userRepository.findAllByDoctorProfileIdAndRoles(referral.getAssignedDoctor().getId(), Set.of(RoleName.DOCTOR));
    }

    private String buildReferralLink(AppUser user, Referral referral) {
        RoleName role = user.getPrimaryRole();
        if (role == RoleName.HOSPITAL_ADMIN) {
            return "/hospital-admin/referrals/" + referral.getId();
        }
        if (role == RoleName.REFERRAL_OFFICER) {
            return "/referral-officer/referrals/" + referral.getId();
        }
        if (role == RoleName.DOCTOR) {
            return "/doctor/referrals/" + referral.getId();
        }
        if (role == RoleName.VIEWER) {
            return "/viewer/reports";
        }
        return "/notifications";
    }

    private void markRead(UserNotification notification) {
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
    }

    private Long requireCurrentUserId() {
        CustomUserDetails user = currentUserFacade.requireUser();
        return user.getUserId();
    }

    private boolean matchesNotification(UserNotification notification, String query) {
        return contains(notification.getTitle(), query)
            || contains(notification.getMessage(), query)
            || (notification.getType() != null && contains(notification.getType().getDisplayName(), query));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.trim().toLowerCase(Locale.ROOT);
    }
}
