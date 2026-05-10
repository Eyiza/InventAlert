package com.inventalert.notificationService.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String notificationId;
    private String companyId;
    private String userId;
    private NotificationType type;
    private String message;
    private String referenceId;
    private boolean read;
    private Instant createdAt;
}
