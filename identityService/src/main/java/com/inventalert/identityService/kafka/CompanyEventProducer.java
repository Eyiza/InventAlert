package com.inventalert.identityService.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompanyEventProducer {

    private static final String TOPIC_COMPANY_CREATED    = "company.created";
    private static final String TOPIC_COMPANY_OFFBOARDED = "company.offboarded";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCompanyCreated(String companyId, String companyName, String adminEmail) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("companyId", companyId);
        event.put("companyName", companyName);
        event.put("adminEmail", adminEmail);
        event.put("timestamp", Instant.now().toString());
        kafkaTemplate.send(TOPIC_COMPANY_CREATED, companyId, event);
    }

    public void publishCompanyOffboarded(String companyId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("companyId", companyId);
        event.put("timestamp", Instant.now().toString());
        kafkaTemplate.send(TOPIC_COMPANY_OFFBOARDED, companyId, event);
    }
}
