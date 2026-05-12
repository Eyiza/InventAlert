package com.inventalert.notificationService.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.notificationService.dto.event.PasswordResetEvent;
import com.inventalert.notificationService.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PasswordResetEventConsumer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final EmailService emailService;
    private final String frontendUrl;

    public PasswordResetEventConsumer(
            EmailService emailService,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @KafkaListener(topics = "password.reset.requested", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        log.info("Received message on password.reset.requested: {}", message);
        try {
            PasswordResetEvent event = MAPPER.readValue(message, PasswordResetEvent.class);
            log.info("Parsed password reset event for userId={}, email={}", event.userId(), event.email());
            String resetLink = frontendUrl + "/reset-password?token=" + event.token();
            String body = "You requested a password reset for your InventAlert account.\n\n"
                    + "Click the link below to set a new password. This link expires at " + event.expiresAt() + ".\n\n"
                    + resetLink + "\n\n"
                    + "If you did not request this, you can safely ignore this email.";
            log.info("Sending password reset email to {}", event.email());
            emailService.sendNotificationEmail(event.email(), "Reset your InventAlert password", body);
            log.info("Password reset email dispatched successfully to {}", event.email());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse password.reset.requested event: {}", e.getMessage());
        }
    }
}
