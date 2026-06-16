package com.example.referrals.web;

import com.example.referrals.notification.NotificationService;
import com.example.referrals.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class NotificationModelAdvice {

    private final NotificationService notificationService;

    public NotificationModelAdvice(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ModelAttribute("unreadNotificationCount")
    public long unreadNotificationCount(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return 0;
        }
        return notificationService.countUnreadForCurrentUser();
    }
}
