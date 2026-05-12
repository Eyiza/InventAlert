package com.inventalert.analyticsService.dto.event;

public record TransferEvent(
        String eventId,
        String companyId,
        String suggestionId,
        String productId,
        String fromWarehouseId,
        String toWarehouseId,
        Integer quantity,
        Double distanceKm,
        String status,
        String timestamp
) {}
