package com.inventalert.analyticsService.dto.event;

public record StockMovementEvent(
        String eventId,
        String companyId,
        String movementId,
        String productId,
        String warehouseId,
        String type,
        int quantity,
        String timestamp
) {}
