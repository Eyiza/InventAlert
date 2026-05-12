package com.inventalert.analyticsService.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.analyticsService.dto.event.CompanyCreatedEvent;
import com.inventalert.analyticsService.dto.event.CompanyOffboardedEvent;
import com.inventalert.analyticsService.service.AnalyticsIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyEventConsumer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AnalyticsIngestionService ingestionService;

    @KafkaListener(topics = "company.created", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeCreated(String message) {
        try {
            CompanyCreatedEvent event = MAPPER.readValue(message, CompanyCreatedEvent.class);
            ingestionService.ingestCompanyCreated(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse company.created event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "company.offboarded", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOffboarded(String message) {
        try {
            CompanyOffboardedEvent event = MAPPER.readValue(message, CompanyOffboardedEvent.class);
            ingestionService.ingestCompanyOffboarded(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse company.offboarded event: {}", e.getMessage());
        }
    }
}
