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
public class TransferEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTransferSuggestionCreated(String companyId, String suggestionId,
                                                   String fromWarehouseId, String toWarehouseId,
                                                   String productId, int quantity, double distanceKm) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("companyId", companyId);
        event.put("suggestionId", suggestionId);
        event.put("fromWarehouseId", fromWarehouseId);
        event.put("toWarehouseId", toWarehouseId);
        event.put("productId", productId);
        event.put("quantity", quantity);
        event.put("distanceKm", distanceKm);
        event.put("status", "SUGGESTED");
        event.put("timestamp", Instant.now().toString());
        kafkaTemplate.send("transfer.suggestion.created", companyId, event);
    }

    public void publishTransferApproved(String companyId, String suggestionId, String approvedBy) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("companyId", companyId);
        event.put("suggestionId", suggestionId);
        event.put("approvedBy", approvedBy);
        event.put("timestamp", Instant.now().toString());
        kafkaTemplate.send("transfer.approved", companyId, event);
    }

    public void publishTransferAccepted(String companyId, String suggestionId, String acceptedBy) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("companyId", companyId);
        event.put("suggestionId", suggestionId);
        event.put("acceptedBy", acceptedBy);
        event.put("timestamp", Instant.now().toString());
        kafkaTemplate.send("transfer.accepted", companyId, event);
    }

    public void publishTransferRejected(String companyId, String suggestionId,
                                         String rejectedBy, String escalatedToAlertId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("companyId", companyId);
        event.put("suggestionId", suggestionId);
        event.put("rejectedBy", rejectedBy);
        event.put("escalatedToAlertId", escalatedToAlertId);
        event.put("timestamp", Instant.now().toString());
        kafkaTemplate.send("transfer.rejected", companyId, event);
    }
}
