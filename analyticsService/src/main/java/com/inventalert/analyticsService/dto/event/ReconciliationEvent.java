package com.inventalert.analyticsService.dto.event;

public record ReconciliationEvent(
        String eventId,
        String companyId,
        String reconciliationId,
        String warehouseId,
        String timestamp
) {}
