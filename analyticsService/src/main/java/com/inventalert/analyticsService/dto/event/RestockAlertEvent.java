package com.inventalert.analyticsService.dto.event;

public record RestockAlertEvent(
        String eventId,
        String companyId,
        String alertId,
        String productId,
        String warehouseId,
        int stockAtAlert,
        int threshold,
        String timestamp
) {}
