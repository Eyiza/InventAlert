package com.inventalert.analyticsService.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.analyticsService.dto.event.TransferEvent;
import com.inventalert.analyticsService.service.AnalyticsIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferEventConsumer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AnalyticsIngestionService ingestionService;

    @KafkaListener(topics = {
            "transfer.suggestion.created",
            "transfer.approved",
            "transfer.rejected",
            "transfer.accepted"
    }, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            TransferEvent event = MAPPER.readValue(message, TransferEvent.class);
            ingestionService.ingestTransferEvent(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse transfer event from topic {}: {}", topic, e.getMessage());
        }
    }
}
