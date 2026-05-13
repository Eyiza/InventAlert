package com.inventalert.notificationService.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PasswordResetEvent(
        String eventId,
        String userId,
        String email,
        String token,
        String expiresAt
) {}
