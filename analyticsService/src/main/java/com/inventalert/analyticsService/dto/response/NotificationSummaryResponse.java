package com.inventalert.analyticsService.dto.response;

import java.util.List;
import java.util.Map;

public record NotificationSummaryResponse(
        long totalNotifications,
        List<Map<String, Object>> breakdownByType,
        List<Map<String, Object>> volumeByDay,
        List<Map<String, Object>> topNotifiedUsers
) {}
