package com.inventalert.analyticsService.dto.response;

import java.util.List;
import java.util.Map;

public record StockSummaryResponse(
        long totalMovements,
        long totalIntake,
        long totalOutbound,
        long totalTransfers,
        List<Map<String, Object>> topMovingProducts,
        List<Map<String, Object>> trendByDay
) {}
