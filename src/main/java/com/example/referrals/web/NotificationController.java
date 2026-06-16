package com.example.referrals.web;

import com.example.referrals.common.model.RoleName;
import com.example.referrals.common.web.CurrentUserFacade;
import com.example.referrals.notification.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserFacade currentUserFacade;

    public NotificationController(NotificationService notificationService,
                                  CurrentUserFacade currentUserFacade) {
        this.notificationService = notificationService;
        this.currentUserFacade = currentUserFacade;
    }

    @GetMapping
    public String inbox(@RequestParam(required = false) String q,
                        @RequestParam(defaultValue = "false") boolean unreadOnly,
                        Model model) {
        model.addAttribute("notifications", notificationService.findCurrentUserNotifications(q, unreadOnly));
        model.addAttribute("q", q);
        model.addAttribute("unreadOnly", unreadOnly);
        populateWorkspaceNavigation(model);
        return "notifications/list";
    }

    @GetMapping("/{notificationId}")
    public String open(@PathVariable Long notificationId) {
        return "redirect:" + notificationService.openNotification(notificationId);
    }

    @PostMapping("/read-all")
    public String markAllRead() {
        notificationService.markAllCurrentUserNotificationsRead();
        return "redirect:/notifications";
    }
