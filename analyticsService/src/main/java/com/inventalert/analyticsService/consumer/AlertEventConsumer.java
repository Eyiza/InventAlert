package com.inventalert.analyticsService.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.analyticsService.dto.event.RestockAlertEvent;
import com.inventalert.analyticsService.service.AnalyticsIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertEventConsumer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AnalyticsIngestionService ingestionService;

    @KafkaListener(topics = "restock.alert.created", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        try {
            RestockAlertEvent event = MAPPER.readValue(message, RestockAlertEvent.class);
            ingestionService.ingestRestockAlert(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse restock.alert.created: {}", e.getMessage());
        }
    }
}
