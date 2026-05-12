package com.inventalert.analyticsService.dto.event;

public record NotificationEvent(
        String eventId,
        String companyId,
        String userId,
        String userEmail,
        String type,
        String message,
        String referenceId
) {}
