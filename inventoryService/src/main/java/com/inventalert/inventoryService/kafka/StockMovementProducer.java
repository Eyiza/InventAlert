package com.inventalert.inventoryService.kafka;

import com.inventalert.inventoryService.model.MovementType;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StockMovementProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishMovementCreated(String companyId, String movementId, String productId,
                                        String warehouseId, MovementType type, int quantity) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("companyId", companyId);
        event.put("movementId", movementId);
        event.put("productId", productId);
        event.put("warehouseId", warehouseId);
        event.put("type", type.name());
        event.put("quantity", quantity);
        event.put("timestamp", Instant.now().toString());
        kafkaTemplate.send("stock.movement.created", companyId, event);
    }
}
