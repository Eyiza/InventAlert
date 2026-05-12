package com.inventalert.analyticsService.service;

import com.inventalert.analyticsService.dto.response.*;
import com.inventalert.analyticsService.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsQueryService {

    private final CompanyEventRepository companyEventRepo;
    private final StockMovementEventRepository stockMovementRepo;
    private final AlertEventRepository alertRepo;
    private final TransferEventRepository transferRepo;
    private final ReconciliationEventRepository reconciliationRepo;
    private final NotificationEventRepository notificationRepo;

    public CompanySummaryResponse getCompanySummary(int months) {
        long active = companyEventRepo.countByEventType("CREATED");
        long offboarded = companyEventRepo.countByEventType("OFFBOARDED");
        long total = active + offboarded;

        List<Map<String, Object>> rawGrowth = companyEventRepo.countByMonthAndEventType("CREATED", months);
        List<MonthlyCountResponse> growth = rawGrowth.stream()
                .map(row -> new MonthlyCountResponse(
                        ((Number) row.get("month")).intValue(),
                        ((Number) row.get("total")).longValue()))
                .toList();

        return new CompanySummaryResponse(total, active, offboarded, growth);
    }

    public StockSummaryResponse getStockSummary(String companyId, Instant from, Instant to) {
        long total = stockMovementRepo.countAll(companyId);
        long intake = stockMovementRepo.countByMovementType(companyId, "INTAKE");
        long outbound = stockMovementRepo.countByMovementType(companyId, "OUTBOUND_SALE");
        long transfers = stockMovementRepo.countByMovementType(companyId, "TRANSFER_OUT");
        List<Map<String, Object>> topProducts = stockMovementRepo.topMovingProducts(companyId, "OUTBOUND_SALE", 10);
        List<Map<String, Object>> trend = stockMovementRepo.movementTrendByDay(companyId, from, to);

        return new StockSummaryResponse(total, intake, outbound, transfers, topProducts, trend);
    }

    public List<Map<String, Object>> getTopMovingProducts(String companyId, String type, int limit) {
        return stockMovementRepo.topMovingProducts(companyId, type, limit);
    }

    public List<Map<String, Object>> getMovementTrendByDay(String companyId, Instant from, Instant to) {
        return stockMovementRepo.movementTrendByDay(companyId, from, to);
    }

    public List<Map<String, Object>> getMovementTrendByWarehouse(String companyId, Instant from, Instant to) {
        return stockMovementRepo.movementTrendByWarehouse(companyId, from, to);
    }

    public AlertSummaryResponse getAlertSummary(String companyId, Instant from, Instant to) {
        long total = alertRepo.countAll(companyId);
        List<Map<String, Object>> byWarehouse = alertRepo.alertFrequencyByWarehouse(companyId, from, to);
        List<Map<String, Object>> byMonth = alertRepo.alertCountByMonth(companyId);

        return new AlertSummaryResponse(total, byWarehouse, byMonth);
    }

    public TransferSummaryResponse getTransferSummary(String companyId, Instant from, Instant to) {
        List<Map<String, Object>> byStatus = transferRepo.transferCountByStatus(companyId);
        long suggested = extractStatusCount(byStatus, "SUGGESTED");
        long approved = extractStatusCount(byStatus, "APPROVED");
        long rejected = extractStatusCount(byStatus, "REJECTED");
        long accepted = extractStatusCount(byStatus, "ACCEPTED");
        double avgDist = transferRepo.avgDistanceKm(companyId);
        List<Map<String, Object>> volumeByProduct = transferRepo.transferVolumeByProduct(companyId, from, to);

        return new TransferSummaryResponse(suggested, approved, rejected, accepted, avgDist, volumeByProduct);
    }

    public NotificationSummaryResponse getNotificationSummary(String companyId, Instant from, Instant to) {
        long total = notificationRepo.countAll(companyId);
        List<Map<String, Object>> byType = notificationRepo.notificationBreakdownByType(companyId, from, to);
        List<Map<String, Object>> byDay = notificationRepo.notificationVolumeByDay(companyId, from, to);
        List<Map<String, Object>> topUsers = notificationRepo.topNotifiedUsers(companyId, 10);

        return new NotificationSummaryResponse(total, byType, byDay, topUsers);
    }

    private long extractStatusCount(List<Map<String, Object>> rows, String status) {
        return rows.stream()
                .filter(r -> status.equals(r.get("status")))
                .mapToLong(r -> ((Number) r.get("total")).longValue())
                .findFirst()
                .orElse(0L);
    }
}
