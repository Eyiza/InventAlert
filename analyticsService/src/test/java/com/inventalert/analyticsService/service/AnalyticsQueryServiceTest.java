package com.inventalert.analyticsService.service;

import com.inventalert.analyticsService.dto.response.*;
import com.inventalert.analyticsService.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsQueryServiceTest {

    @Mock CompanyEventRepository companyEventRepo;
    @Mock StockMovementEventRepository stockMovementRepo;
    @Mock AlertEventRepository alertRepo;
    @Mock TransferEventRepository transferRepo;
    @Mock ReconciliationEventRepository reconciliationRepo;
    @Mock NotificationEventRepository notificationRepo;

    @InjectMocks
    AnalyticsQueryService service;

    // ── Company Summary ───────────────────────────────────────────────────────

    @Test
    void getCompanySummary_emptyData_returnsZeroTotals() {
        when(companyEventRepo.countByEventType("CREATED")).thenReturn(0L);
        when(companyEventRepo.countByEventType("OFFBOARDED")).thenReturn(0L);
        when(companyEventRepo.countByMonthAndEventType("CREATED", 6)).thenReturn(List.of());

        CompanySummaryResponse result = service.getCompanySummary(6);

        assertThat(result.totalCompanies()).isZero();
        assertThat(result.activeCompanies()).isZero();
        assertThat(result.offboardedCompanies()).isZero();
        assertThat(result.growthByMonth()).isEmpty();
    }

    @Test
    void getCompanySummary_withData_computesTotalsCorrectly() {
        when(companyEventRepo.countByEventType("CREATED")).thenReturn(10L);
        when(companyEventRepo.countByEventType("OFFBOARDED")).thenReturn(2L);
        when(companyEventRepo.countByMonthAndEventType("CREATED", 3))
                .thenReturn(List.of(Map.of("month", 202501, "total", 5L),
                                    Map.of("month", 202502, "total", 5L)));

        CompanySummaryResponse result = service.getCompanySummary(3);

        assertThat(result.totalCompanies()).isEqualTo(12L);
        assertThat(result.activeCompanies()).isEqualTo(10L);
        assertThat(result.offboardedCompanies()).isEqualTo(2L);
        assertThat(result.growthByMonth()).hasSize(2);
        assertThat(result.growthByMonth().get(0).yearMonth()).isEqualTo(202501);
        assertThat(result.growthByMonth().get(0).count()).isEqualTo(5L);
    }

    // ── Stock Summary ─────────────────────────────────────────────────────────

    @Test
    void getStockSummary_emptyData_returnsZeroTotals() {
        String companyId = "co-1";
        Instant from = Instant.parse("2025-01-01T00:00:00Z");
        Instant to = Instant.parse("2025-01-31T00:00:00Z");

        when(stockMovementRepo.countAll(companyId, null)).thenReturn(0L);
        when(stockMovementRepo.countByMovementType(companyId, "INTAKE", null)).thenReturn(0L);
        when(stockMovementRepo.countByMovementType(companyId, "OUTBOUND_SALE", null)).thenReturn(0L);
        when(stockMovementRepo.countByMovementType(companyId, "TRANSFER_OUT", null)).thenReturn(0L);
        when(stockMovementRepo.topMovingProducts(eq(companyId), eq("OUTBOUND_SALE"), eq(10), isNull())).thenReturn(List.of());
        when(stockMovementRepo.movementTrendByDay(eq(companyId), any(), any(), isNull())).thenReturn(List.of());

        StockSummaryResponse result = service.getStockSummary(companyId, from, to, null);

        assertThat(result.totalMovements()).isZero();
        assertThat(result.totalIntake()).isZero();
        assertThat(result.totalOutbound()).isZero();
        assertThat(result.totalTransfers()).isZero();
        assertThat(result.topMovingProducts()).isEmpty();
        assertThat(result.trendByDay()).isEmpty();
    }

    @Test
    void getStockSummary_withData_mapsCountsCorrectly() {
        String companyId = "co-1";
        Instant from = Instant.parse("2025-01-01T00:00:00Z");
        Instant to = Instant.parse("2025-01-31T00:00:00Z");

        when(stockMovementRepo.countAll(companyId, null)).thenReturn(300L);
        when(stockMovementRepo.countByMovementType(companyId, "INTAKE", null)).thenReturn(100L);
        when(stockMovementRepo.countByMovementType(companyId, "OUTBOUND_SALE", null)).thenReturn(150L);
        when(stockMovementRepo.countByMovementType(companyId, "TRANSFER_OUT", null)).thenReturn(50L);
        when(stockMovementRepo.topMovingProducts(any(), any(), anyInt(), any()))
                .thenReturn(List.of(Map.of("productId", "prod-1", "totalQty", 150L)));
        when(stockMovementRepo.movementTrendByDay(any(), any(), any(), any())).thenReturn(List.of());

        StockSummaryResponse result = service.getStockSummary(companyId, from, to, null);

        assertThat(result.totalMovements()).isEqualTo(300L);
        assertThat(result.totalIntake()).isEqualTo(100L);
        assertThat(result.totalOutbound()).isEqualTo(150L);
        assertThat(result.totalTransfers()).isEqualTo(50L);
    }

    // ── Transfer Summary ──────────────────────────────────────────────────────

    @Test
    void getTransferSummary_withStatusRows_mapsStatusCountsCorrectly() {
        String companyId = "co-1";
        Instant from = Instant.parse("2025-01-01T00:00:00Z");
        Instant to = Instant.parse("2025-01-31T00:00:00Z");

        when(transferRepo.transferCountByStatus(companyId, null)).thenReturn(List.of(
                Map.of("status", "SUGGESTED", "total", 10L),
                Map.of("status", "APPROVED", "total", 7L),
                Map.of("status", "REJECTED", "total", 2L),
                Map.of("status", "ACCEPTED", "total", 5L)
        ));
        when(transferRepo.avgDistanceKm(companyId, null)).thenReturn(42.5);
        when(transferRepo.transferVolumeByProduct(any(), any(), any(), any())).thenReturn(List.of());

        TransferSummaryResponse result = service.getTransferSummary(companyId, from, to, null);

        assertThat(result.totalSuggested()).isEqualTo(10L);
        assertThat(result.totalApproved()).isEqualTo(7L);
        assertThat(result.totalRejected()).isEqualTo(2L);
        assertThat(result.totalCompleted()).isEqualTo(5L);
        assertThat(result.avgDistanceKm()).isEqualTo(42.5);
    }

    @Test
    void getTransferSummary_noStatuses_returnsAllZeros() {
        String companyId = "co-1";
        Instant from = Instant.parse("2025-01-01T00:00:00Z");
        Instant to = Instant.parse("2025-01-31T00:00:00Z");

        when(transferRepo.transferCountByStatus(companyId, null)).thenReturn(List.of());
        when(transferRepo.avgDistanceKm(companyId, null)).thenReturn(0.0);
        when(transferRepo.transferVolumeByProduct(any(), any(), any(), any())).thenReturn(List.of());

        TransferSummaryResponse result = service.getTransferSummary(companyId, from, to, null);

        assertThat(result.totalSuggested()).isZero();
        assertThat(result.totalApproved()).isZero();
        assertThat(result.totalRejected()).isZero();
        assertThat(result.totalCompleted()).isZero();
    }

    // ── Notification Summary ──────────────────────────────────────────────────

    @Test
    void getNotificationSummary_emptyData_returnsZeroTotal() {
        String companyId = "co-1";
        Instant from = Instant.parse("2025-01-01T00:00:00Z");
        Instant to = Instant.parse("2025-01-31T00:00:00Z");

        when(notificationRepo.countAll(companyId)).thenReturn(0L);
        when(notificationRepo.notificationBreakdownByType(any(), any(), any())).thenReturn(List.of());
        when(notificationRepo.notificationVolumeByDay(any(), any(), any())).thenReturn(List.of());
        when(notificationRepo.topNotifiedUsers(eq(companyId), eq(10))).thenReturn(List.of());

        NotificationSummaryResponse result = service.getNotificationSummary(companyId, from, to);

        assertThat(result.totalNotifications()).isZero();
        assertThat(result.breakdownByType()).isEmpty();
        assertThat(result.volumeByDay()).isEmpty();
        assertThat(result.topNotifiedUsers()).isEmpty();
    }
}
