package com.example.referrals.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    @Query("""
        select n
        from UserNotification n
        left join fetch n.referral r
        where n.recipient.id = :recipientUserId
        order by n.createdAt desc
        """)
    List<UserNotification> findAllByRecipientUserId(Long recipientUserId);

    @Query("""
        select n
        from UserNotification n
        left join fetch n.referral r
        where n.id = :notificationId and n.recipient.id = :recipientUserId
        """)
    Optional<UserNotification> findVisibleById(Long notificationId, Long recipientUserId);

    long countByRecipientIdAndReadAtIsNull(Long recipientUserId);

    List<UserNotification> findAllByRecipientIdAndReadAtIsNull(Long recipientUserId);
}
