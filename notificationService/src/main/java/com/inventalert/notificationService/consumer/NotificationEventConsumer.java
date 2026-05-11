package com.inventalert.notificationService.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.notificationService.dto.event.NotificationEvent;
import com.inventalert.notificationService.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification.events", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        try {
            NotificationEvent event = objectMapper.readValue(message, NotificationEvent.class);
            notificationService.create(
                    event.eventId(), event.companyId(), event.userId(),
                    event.type(), event.message(), event.referenceId()
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to parse notification event: {}", e.getMessage());
        }
    }
}
