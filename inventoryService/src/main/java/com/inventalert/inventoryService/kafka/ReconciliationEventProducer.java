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
public class ReconciliationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishReconciliationRequested(String companyId, String reconciliationId,
                                                String warehouseId, String submittedBy) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("companyId", companyId);
        event.put("reconciliationId", reconciliationId);
        event.put("warehouseId", warehouseId);
        event.put("submittedBy", submittedBy);
        event.put("timestamp", Instant.now().toString());
        kafkaTemplate.send("reconciliation.requested", companyId, event);
    }
}
