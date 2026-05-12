package com.inventalert.analyticsService.controller;

import com.inventalert.analyticsService.dto.response.CompanySummaryResponse;
import com.inventalert.analyticsService.service.AnalyticsQueryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/companies")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CompanyAnalyticsController {

    private final AnalyticsQueryService queryService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CompanySummaryResponse> getSummary(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(queryService.getCompanySummary(months));
    }
}
