package com.inventalert.notificationService.service;

import com.inventalert.notificationService.dto.response.NotificationResponse;
import com.inventalert.notificationService.dto.response.UnreadCountResponse;
import com.inventalert.notificationService.model.Notification;
import com.inventalert.notificationService.model.NotificationType;

import java.util.List;

public interface NotificationService {
    Notification create(String eventId, String companyId, String userId, String userEmail,
                        NotificationType type, String message, String referenceId);

    List<NotificationResponse> getNotifications(String companyId, String userId, int page, int size);

    NotificationResponse markAsRead(String companyId, String notificationId);

    UnreadCountResponse getUnreadCount(String companyId, String userId);
}
