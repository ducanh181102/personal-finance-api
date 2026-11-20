package com.example.personalfinance.repository;

import com.example.personalfinance.model.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Integer> {
    Optional<AppNotification> findByUserIdAndDeduplicationKey(int userId, String deduplicationKey);

    Optional<AppNotification> findByIdAndUserId(int id, int userId);

    List<AppNotification> findTop100ByUserIdOrderByCreatedAtDesc(int userId);

    List<AppNotification> findByUserIdAndReadAtIsNull(int userId);

    long countByUserIdAndReadAtIsNull(int userId);
}
