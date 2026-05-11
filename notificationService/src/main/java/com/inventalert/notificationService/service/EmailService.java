package com.inventalert.notificationService.service;

public interface EmailService {
    void sendNotificationEmail(String to, String subject, String body);
}
