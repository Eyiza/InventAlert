package com.inventalert.notificationService.dto.event;

import com.inventalert.notificationService.model.NotificationType;

public record NotificationEvent(
        String eventId,
        String companyId,
        String userId,
        NotificationType type,
        String message,
        String referenceId
) {}
