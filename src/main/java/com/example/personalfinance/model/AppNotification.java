package com.example.personalfinance.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Một thông báo nghiệp vụ thuộc về đúng một người dùng.
 * deduplicationKey ngăn một cảnh báo còn hiệu lực tạo lại nhiều lần mỗi khi người dùng mở trang.
 */
@Entity
@Table(
        name = "app_notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_user_deduplication_key",
                columnNames = {"user_id", "deduplication_key"}
        ),
        indexes = @Index(name = "idx_notification_user_created_at", columnList = "user_id,created_at")
)
public class AppNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(nullable = false, length = 255)
    private String targetPath;

    @Column(name = "deduplication_key", nullable = false, length = 160)
    private String deduplicationKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected AppNotification() {
    }

    public AppNotification(User user, NotificationType type, String title, String body,
                           String targetPath, String deduplicationKey, Instant createdAt) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.body = body;
        this.targetPath = targetPath;
        this.deduplicationKey = deduplicationKey;
        this.createdAt = createdAt;
    }

    public void markRead(Instant readAt) {
        if (this.readAt == null) this.readAt = readAt;
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

    public Instant getReadAt() {
        return readAt;
    }
}
