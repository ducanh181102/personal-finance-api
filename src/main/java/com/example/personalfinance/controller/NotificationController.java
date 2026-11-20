package com.example.personalfinance.controller;

import com.example.personalfinance.dto.notification.NotificationListResponse;
import com.example.personalfinance.dto.notification.NotificationResponse;
import com.example.personalfinance.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationListResponse getNotifications() {
        return notificationService.getCurrentUserNotifications();
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable int id) {
        return notificationService.markCurrentUserNotificationRead(id);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllCurrentUserNotificationsRead();
        return ResponseEntity.noContent().build();
    }
}
