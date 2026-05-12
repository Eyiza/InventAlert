package com.inventalert.analyticsService.controller;

import com.inventalert.analyticsService.security.model.JwtUser;
import com.inventalert.analyticsService.service.AnalyticsQueryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

@RestController
@RequestMapping("/api/analytics/stock")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StockAnalyticsController {

    private final AnalyticsQueryService queryService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> getSummary(
            @AuthenticationPrincipal JwtUser user,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant[] range = resolveDateRange(from, to);
        return ResponseEntity.ok(queryService.getStockSummary(user.getCompanyId(), range[0], range[1]));
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> getTopProducts(
            @AuthenticationPrincipal JwtUser user,
            @RequestParam(defaultValue = "OUTBOUND_SALE") String type,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(queryService.getTopMovingProducts(user.getCompanyId(), type, limit));
    }

    @GetMapping("/movements/trend")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> getMovementTrend(
            @AuthenticationPrincipal JwtUser user,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant[] range = resolveDateRange(from, to);
        return ResponseEntity.ok(queryService.getMovementTrendByDay(user.getCompanyId(), range[0], range[1]));
    }

    @GetMapping("/movements/by-warehouse")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> getMovementByWarehouse(
            @AuthenticationPrincipal JwtUser user,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant[] range = resolveDateRange(from, to);
        return ResponseEntity.ok(queryService.getMovementTrendByWarehouse(user.getCompanyId(), range[0], range[1]));
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
