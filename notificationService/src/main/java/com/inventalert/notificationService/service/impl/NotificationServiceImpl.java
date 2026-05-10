package com.inventalert.notificationService.service.impl;

import com.inventalert.notificationService.dto.response.NotificationResponse;
import com.inventalert.notificationService.dto.response.UnreadCountResponse;
import com.inventalert.notificationService.exception.NotificationNotFoundException;
import com.inventalert.notificationService.model.Notification;
import com.inventalert.notificationService.model.NotificationType;
import com.inventalert.notificationService.repository.RedisNotificationRepository;
import com.inventalert.notificationService.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final RedisNotificationRepository repository;

    @Override
    public Notification create(String eventId,
                               String companyId,
                               String userId,
                               NotificationType type,
                               String message,
                               String referenceId) {

        if (!repository.setEventProcessedIfAbsent(eventId)) {
            return null;
        }

        Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .companyId(companyId)
                .userId(userId)
                .type(type)
                .message(message)
                .referenceId(referenceId)
                .read(false)
                .createdAt(Instant.now())
                .build();

        repository.saveHash(notification);
        repository.addToUserSortedSet(
                companyId, userId,
                notification.getNotificationId(),
                notification.getCreatedAt().toEpochMilli());
        repository.incrementUnreadCount(companyId, userId);

        return notification;
    }

    @Override
    public List<NotificationResponse> getNotifications(String companyId, String userId, int page, int size) {
        long start = (long) page * size;
        long stop  = start + size - 1;

        Set<String> ids = repository.getNotificationIds(companyId, userId, start, stop);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return ids.stream()
                .map(id -> repository.getHash(companyId, id))
                .filter(hash -> hash != null && !hash.isEmpty())
                .map(this::toResponse)
                .toList();
    }

    @Override
    public NotificationResponse markAsRead(String companyId, String notificationId) {
        Map<String, String> hash = repository.getHash(companyId, notificationId);
        if (hash == null || hash.isEmpty()) {
            throw new NotificationNotFoundException(notificationId);
        }

        if (!"1".equals(hash.get("isRead"))) {
            repository.markHashAsRead(companyId, notificationId);
            repository.decrementUnreadCount(companyId, hash.get("userId"));
        }

        return new NotificationResponse(
                hash.get("notificationId"),
                hash.get("companyId"),
                hash.get("userId"),
                NotificationType.valueOf(hash.get("type")),
                hash.get("message"),
                hash.get("referenceId"),
                true,
                Instant.parse(hash.get("createdAt"))
        );
    }

    @Override
    public UnreadCountResponse getUnreadCount(String companyId, String userId) {
        return new UnreadCountResponse(repository.getUnreadCount(companyId, userId));
    }

    private NotificationResponse toResponse(Map<String, String> hash) {
        return new NotificationResponse(
                hash.get("notificationId"),
                hash.get("companyId"),
                hash.get("userId"),
                NotificationType.valueOf(hash.get("type")),
                hash.get("message"),
                hash.get("referenceId"),
                "1".equals(hash.get("isRead")),
                Instant.parse(hash.get("createdAt"))
        );
    }
}
