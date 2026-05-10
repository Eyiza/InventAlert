package com.inventalert.notificationService.service;

import com.inventalert.notificationService.model.Notification;
import com.inventalert.notificationService.model.NotificationType;

public interface NotificationService {
    Notification create(String eventId, String companyId, String userId,
                        NotificationType type, String message, String referenceId);
}
