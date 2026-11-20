package com.example.personalfinance.dto.notification;

import java.util.List;

public class NotificationListResponse {
    private final List<NotificationResponse> notifications;
    private final long unreadCount;

    public NotificationListResponse(List<NotificationResponse> notifications, long unreadCount) {
        this.notifications = notifications;
        this.unreadCount = unreadCount;
    }

    public List<NotificationResponse> getNotifications() {
        return notifications;
    }

    public long getUnreadCount() {
        return unreadCount;
    }
}
