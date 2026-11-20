package com.example.personalfinance.dto.notification;

import com.example.personalfinance.model.NotificationType;

import java.time.Instant;

public class NotificationResponse {
    private final int id;
    private final NotificationType type;
    private final String title;
    private final String body;
    private final String targetPath;
    private final Instant createdAt;
    private final boolean read;

    public NotificationResponse(int id, NotificationType type, String title, String body,
                                String targetPath, Instant createdAt, boolean read) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.body = body;
        this.targetPath = targetPath;
        this.createdAt = createdAt;
        this.read = read;
    }

    public int getId() {
        return id;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }
}
