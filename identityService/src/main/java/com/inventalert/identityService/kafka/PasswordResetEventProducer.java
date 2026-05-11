package com.inventalert.identityService.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PasswordResetEventProducer {

    private static final String TOPIC = "password.reset.requested";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPasswordResetRequested(String userId, String email, String token, LocalDateTime expiresAt) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("userId", userId);
        event.put("email", email);
        event.put("token", token);
        event.put("expiresAt", expiresAt.toString());
        event.put("timestamp", Instant.now().toString());
        kafkaTemplate.send(TOPIC, userId, event);
    }
}
