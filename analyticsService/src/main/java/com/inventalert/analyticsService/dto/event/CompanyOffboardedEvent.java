package com.inventalert.analyticsService.dto.event;

public record CompanyOffboardedEvent(
        String eventId,
        String companyId,
        String timestamp
) {}
