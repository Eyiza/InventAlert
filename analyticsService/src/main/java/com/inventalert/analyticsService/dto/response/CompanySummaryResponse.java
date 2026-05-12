package com.inventalert.analyticsService.dto.response;

import java.util.List;

public record CompanySummaryResponse(
        long totalCompanies,
        long activeCompanies,
        long offboardedCompanies,
        List<MonthlyCountResponse> growthByMonth
) {}
