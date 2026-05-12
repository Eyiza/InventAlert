package com.inventalert.analyticsService.controller;

import com.inventalert.analyticsService.dto.response.NotificationSummaryResponse;
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

@RestController
@RequestMapping("/api/analytics/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NotificationAnalyticsController {

    private final AnalyticsQueryService queryService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<NotificationSummaryResponse> getSummary(
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
        return ResponseEntity.ok(queryService.getNotificationSummary(user.getCompanyId(), fromInstant, toInstant));
    }
}
