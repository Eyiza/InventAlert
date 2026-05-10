package com.inventalert.notificationService.service.impl;

import com.inventalert.notificationService.model.Notification;
import com.inventalert.notificationService.model.NotificationType;
import com.inventalert.notificationService.repository.RedisNotificationRepository;
import com.inventalert.notificationService.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
}
