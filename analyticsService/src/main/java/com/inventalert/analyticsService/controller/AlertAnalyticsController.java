package com.inventalert.analyticsService.controller;

import com.inventalert.analyticsService.dto.response.AlertSummaryResponse;
import com.inventalert.analyticsService.security.model.JwtUser;
import com.inventalert.analyticsService.service.AnalyticsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Tag(name = "Alert Analytics", description = "Restock alert trends: total raised, resolved, average resolution time, and per-warehouse breakdowns")
@RestController
@RequestMapping("/api/analytics/alerts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AlertAnalyticsController {

    private final AnalyticsQueryService queryService;

    @Operation(summary = "Alert summary", description = "Total alerts raised, resolved, and average resolution time. Procurement officers see company-wide; managers see their warehouse only.")
    @ApiResponse(responseCode = "200", description = "Alert summary")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public ResponseEntity<AlertSummaryResponse> getSummary(
            @AuthenticationPrincipal JwtUser user,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant[] range = resolveDateRange(from, to);
        String warehouseId = "ADMIN".equals(user.getRole()) ? null : user.getWarehouseId();
        return ResponseEntity.ok(queryService.getAlertSummary(user.getCompanyId(), range[0], range[1], warehouseId));
    }

    @Operation(summary = "Alerts by warehouse", description = "Alert counts broken down by warehouse to identify which locations experience the most stockouts.")
    @ApiResponse(responseCode = "200", description = "Per-warehouse alert breakdown")
    @GetMapping("/by-warehouse")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER')")
    public ResponseEntity<List<Map<String, Object>>> getByWarehouse(
            @AuthenticationPrincipal JwtUser user,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant[] range = resolveDateRange(from, to);
        String warehouseId = "ADMIN".equals(user.getRole()) ? null : user.getWarehouseId();
        return ResponseEntity.ok(
                queryService.getAlertSummary(user.getCompanyId(), range[0], range[1], warehouseId).alertsByWarehouse());
    }

    private Instant[] resolveDateRange(String from, String to) {
        Instant toInstant = (to != null) ? Instant.parse(to) : Instant.now();
        Instant fromInstant = (from != null) ? Instant.parse(from) : toInstant.minus(30, ChronoUnit.DAYS);
        if (fromInstant.isAfter(toInstant)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        }
        if (ChronoUnit.DAYS.between(fromInstant, toInstant) > 365) {
            throw new IllegalArgumentException("Date range cannot exceed 1 year");
        }
        return new Instant[]{fromInstant, toInstant};
    }
}
