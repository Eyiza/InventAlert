package com.inventalert.analyticsService.dto.response;

import java.util.List;
import java.util.Map;

public record AlertSummaryResponse(
        long totalAlerts,
        List<Map<String, Object>> alertsByWarehouse,
        List<Map<String, Object>> alertsByMonth
) {}
