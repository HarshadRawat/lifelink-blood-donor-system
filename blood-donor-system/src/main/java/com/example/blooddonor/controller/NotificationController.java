package com.example.blooddonor.controller;
import java.util.Map;

import com.example.blooddonor.dto.NotificationResponse;
import com.example.blooddonor.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getMyNotifications(
            Authentication authentication
    ) {

        Long userId = (Long) authentication.getPrincipal();

        return notificationService.getMyNotifications(userId);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {

        Long userId = (Long) authentication.getPrincipal();

        return notificationService.markAsRead(
                notificationId,
                userId
        );
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadNotificationCount(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        long count =
                notificationService.getUnreadNotificationCount(userId);

        return Map.of("unreadCount", count);
    }

}