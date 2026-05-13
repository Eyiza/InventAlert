package com.inventalert.analyticsService.repository;

import com.inventalert.analyticsService.dto.event.TransferEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class TransferEventRepository {

    private final JdbcTemplate clickHouseJdbcTemplate;

    public boolean existsByEventId(String eventId) {
        Integer count = clickHouseJdbcTemplate.queryForObject(
                "SELECT count() FROM transfer_events WHERE eventId = ?",
                Integer.class, eventId);
        return count != null && count > 0;
    }

    public void insert(TransferEvent e, Instant eventTime) {
        clickHouseJdbcTemplate.update(
                "INSERT INTO transfer_events " +
                "(eventId, companyId, suggestionId, productId, fromWarehouseId, toWarehouseId, quantity, distanceKm, status, eventTime) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                e.eventId(), e.companyId(), e.suggestionId(), e.productId(),
                e.fromWarehouseId(), e.toWarehouseId(), e.quantity(), e.distanceKm(),
                e.status(), LocalDateTime.ofInstant(eventTime, ZoneOffset.UTC));
    }

    public List<Map<String, Object>> transferCountByStatus(String companyId) {
        return clickHouseJdbcTemplate.queryForList(
                "SELECT status, count() AS total FROM transfer_events " +
                "WHERE companyId = ? GROUP BY status",
                companyId);
    }

    public double avgDistanceKm(String companyId) {
        Double avg = clickHouseJdbcTemplate.queryForObject(
                "SELECT avg(distanceKm) FROM transfer_events " +
                "WHERE companyId = ? AND status = 'SUGGESTED' AND distanceKm IS NOT NULL",
                Double.class, companyId);
        return avg != null ? avg : 0.0;
    }

    public List<Map<String, Object>> transferVolumeByProduct(String companyId, Instant from, Instant to) {
        return clickHouseJdbcTemplate.queryForList(
                "SELECT productId, sum(quantity) AS totalQty FROM transfer_events " +
                "WHERE companyId = ? AND status = 'SUGGESTED' AND eventTime BETWEEN ? AND ? " +
                "GROUP BY productId ORDER BY totalQty DESC",
                companyId, LocalDateTime.ofInstant(from, ZoneOffset.UTC), LocalDateTime.ofInstant(to, ZoneOffset.UTC));
    }
}
