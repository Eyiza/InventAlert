package com.inventalert.inventoryService.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AlertEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishAlertCreated(String companyId, String alertId, String productId,
                                     String warehouseId, String assignedTo, int stockAtAlert, int threshold) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("companyId", companyId);
        event.put("alertId", alertId);
        event.put("productId", productId);
        event.put("warehouseId", warehouseId);
        event.put("stockAtAlert", stockAtAlert);
        event.put("threshold", threshold);
        event.put("timestamp", Instant.now().toString());
        kafkaTemplate.send("restock.alert.created", companyId, event);
    }

    public void publishNotificationEvent(String companyId, String userId, String userEmail,
                                          String type, String message, String referenceId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("companyId", companyId);
        event.put("userId", userId);
        event.put("userEmail", userEmail);
        event.put("type", type);
        event.put("message", message);
        event.put("referenceId", referenceId);
        kafkaTemplate.send("notification.events", companyId, event);
    }
}
