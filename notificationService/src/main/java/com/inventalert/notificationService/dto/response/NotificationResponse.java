package com.inventalert.notificationService.dto.response;

import com.inventalert.notificationService.model.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        String notificationId,
        String companyId,
        String userId,
        NotificationType type,
        String message,
        String referenceId,
        boolean read,
        Instant createdAt
) {}
