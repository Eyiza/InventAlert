package com.inventalert.analyticsService.dto.event;

public record CompanyCreatedEvent(
        String eventId,
        String companyId,
        String companyName,
        String adminEmail,
        String timestamp
) {}
