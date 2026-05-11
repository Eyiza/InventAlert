package com.inventalert.notificationService.service.impl;

import com.inventalert.notificationService.dto.response.NotificationResponse;
import com.inventalert.notificationService.model.Notification;
import com.inventalert.notificationService.service.NotificationBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationBroadcasterImpl implements NotificationBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcast(Notification notification) {
        String destination = "/topic/notifications/%s/%s"
                .formatted(notification.getCompanyId(), notification.getUserId());

        messagingTemplate.convertAndSend(destination, new NotificationResponse(
                notification.getNotificationId(),
                notification.getCompanyId(),
                notification.getUserId(),
                notification.getType(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt()
        ));
    }
}
