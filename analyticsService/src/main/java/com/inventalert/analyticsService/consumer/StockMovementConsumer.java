package com.inventalert.analyticsService.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.analyticsService.dto.event.StockMovementEvent;
import com.inventalert.analyticsService.service.AnalyticsIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockMovementConsumer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AnalyticsIngestionService ingestionService;

    @KafkaListener(topics = "stock.movement.created", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        try {
            StockMovementEvent event = MAPPER.readValue(message, StockMovementEvent.class);
            ingestionService.ingestStockMovement(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse stock.movement.created: {}", e.getMessage());
        }
    }
}
