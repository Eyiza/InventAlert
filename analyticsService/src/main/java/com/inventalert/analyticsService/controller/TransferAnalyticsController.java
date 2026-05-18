package com.inventalert.analyticsService.controller;

import com.inventalert.analyticsService.dto.response.TransferSummaryResponse;
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

@Tag(name = "Transfer Analytics", description = "Inter-warehouse transfer efficiency: completion rate, average distance, and status distribution")
@RestController
@RequestMapping("/api/analytics/transfers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TransferAnalyticsController {

    private final AnalyticsQueryService queryService;

    @Operation(summary = "Transfer summary", description = "Total transfers suggested, approved, completed, and rejected for the period. Includes average distance (km) across completed transfers.")
    @ApiResponse(responseCode = "200", description = "Transfer summary")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<TransferSummaryResponse> getSummary(
            @AuthenticationPrincipal JwtUser user,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant toInstant = (to != null) ? Instant.parse(to) : Instant.now();
        Instant fromInstant = (from != null) ? Instant.parse(from) : toInstant.minus(30, ChronoUnit.DAYS);
        if (fromInstant.isAfter(toInstant)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        }
        if (ChronoUnit.DAYS.between(fromInstant, toInstant) > 365) {
            throw new IllegalArgumentException("Date range cannot exceed 1 year");
        }
        String warehouseId = "ADMIN".equals(user.getRole()) ? null : user.getWarehouseId();
        return ResponseEntity.ok(queryService.getTransferSummary(user.getCompanyId(), fromInstant, toInstant, warehouseId));
    }
}
