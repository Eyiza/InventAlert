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
                                          String alertId, int stockAtAlert, int threshold) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("companyId", companyId);
        event.put("userId", userId);
        event.put("userEmail", userEmail);
        event.put("type", "RESTOCK_ALERT");
        event.put("message", "Low stock alert: stock has dropped to " + stockAtAlert
                + " (threshold: " + threshold + "). Immediate restocking may be required.");
        event.put("referenceId", alertId);
        kafkaTemplate.send("notification.events", companyId, event);
    }
}
