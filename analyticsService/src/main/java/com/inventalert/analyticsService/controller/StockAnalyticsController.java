package com.inventalert.analyticsService.controller;

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

@Tag(name = "Stock Analytics", description = "Time-series stock movement analytics: summaries, top products, daily trends, and per-warehouse breakdowns")
@RestController
@RequestMapping("/api/analytics/stock")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StockAnalyticsController {

    private final AnalyticsQueryService queryService;

    @Operation(summary = "Stock movement summary", description = "Total intake, outbound, and net change for the period (default: last 30 days). MANAGER scope is limited to their warehouse.")
    @ApiResponse(responseCode = "200", description = "Stock summary")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> getSummary(
            @AuthenticationPrincipal JwtUser user,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant[] range = resolveDateRange(from, to);
        String warehouseId = "ADMIN".equals(user.getRole()) ? null : user.getWarehouseId();
        return ResponseEntity.ok(queryService.getStockSummary(user.getCompanyId(), range[0], range[1], warehouseId));
    }

    @Operation(summary = "Top moving products", description = "Most frequently moved products ranked by quantity. Use type=OUTBOUND_SALE (default) or INTAKE.")
    @ApiResponse(responseCode = "200", description = "Top products list")
    @GetMapping("/top-products")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> getTopProducts(
            @AuthenticationPrincipal JwtUser user,
            @RequestParam(defaultValue = "OUTBOUND_SALE") String type,
            @RequestParam(defaultValue = "10") int limit) {
        String warehouseId = "ADMIN".equals(user.getRole()) ? null : user.getWarehouseId();
        return ResponseEntity.ok(queryService.getTopMovingProducts(user.getCompanyId(), type, limit, warehouseId));
    }

    @Operation(summary = "Daily movement trend", description = "Intake and outbound quantities grouped by day for identifying seasonal patterns or anomalies.")
    @ApiResponse(responseCode = "200", description = "Daily movement trend")
    @GetMapping("/movements/trend")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> getMovementTrend(
            @AuthenticationPrincipal JwtUser user,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant[] range = resolveDateRange(from, to);
        String warehouseId = "ADMIN".equals(user.getRole()) ? null : user.getWarehouseId();
        return ResponseEntity.ok(queryService.getMovementTrendByDay(user.getCompanyId(), range[0], range[1], warehouseId));
    }

    @Operation(summary = "Movement breakdown by warehouse", description = "Intake and outbound quantities grouped by warehouse for cross-warehouse comparison.")
    @ApiResponse(responseCode = "200", description = "Per-warehouse movement breakdown")
    @GetMapping("/movements/by-warehouse")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> getMovementByWarehouse(
            @AuthenticationPrincipal JwtUser user,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant[] range = resolveDateRange(from, to);
        String warehouseId = "ADMIN".equals(user.getRole()) ? null : user.getWarehouseId();
        return ResponseEntity.ok(queryService.getMovementTrendByWarehouse(user.getCompanyId(), range[0], range[1], warehouseId));
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
