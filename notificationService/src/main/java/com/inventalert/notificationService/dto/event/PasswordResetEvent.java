package com.inventalert.notificationService.dto.event;

public record PasswordResetEvent(
        String eventId,
        String userId,
        String email,
        String token,
        String expiresAt
) {}
