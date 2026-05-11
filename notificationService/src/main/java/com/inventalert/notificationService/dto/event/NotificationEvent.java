package com.inventalert.notificationService.dto.event;

import com.inventalert.notificationService.model.NotificationType;

public record NotificationEvent(
        String eventId,
        String companyId,
        String userId,
        String userEmail,
        NotificationType type,
        String message,
        String referenceId
) {}
