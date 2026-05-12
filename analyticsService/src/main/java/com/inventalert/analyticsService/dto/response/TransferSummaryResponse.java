package com.inventalert.analyticsService.dto.response;

import java.util.List;
import java.util.Map;

public record TransferSummaryResponse(
        long totalSuggested,
        long totalApproved,
        long totalRejected,
        long totalCompleted,
        double avgDistanceKm,
        List<Map<String, Object>> volumeByProduct
) {}
