package com.inventalert.notificationService.service;

import com.inventalert.notificationService.model.Notification;

public interface NotificationBroadcaster {
    void broadcast(Notification notification);
}
