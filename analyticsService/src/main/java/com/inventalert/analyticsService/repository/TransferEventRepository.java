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

    public List<Map<String, Object>> transferCountByStatus(String companyId, String warehouseId) {
        String sql = "SELECT status, count() AS total FROM transfer_events " +
                     "WHERE companyId = ?" +
                     (warehouseId != null ? " AND (fromWarehouseId = ? OR toWarehouseId = ?)" : "") +
                     " GROUP BY status";
        return warehouseId != null
                ? clickHouseJdbcTemplate.queryForList(sql, companyId, warehouseId, warehouseId)
                : clickHouseJdbcTemplate.queryForList(sql, companyId);
    }

    public double avgDistanceKm(String companyId, String warehouseId) {
        String sql = "SELECT avg(distanceKm) FROM transfer_events " +
                     "WHERE companyId = ? AND status = 'SUGGESTED' AND distanceKm IS NOT NULL" +
                     (warehouseId != null ? " AND (fromWarehouseId = ? OR toWarehouseId = ?)" : "");
        Double avg = warehouseId != null
                ? clickHouseJdbcTemplate.queryForObject(sql, Double.class, companyId, warehouseId, warehouseId)
                : clickHouseJdbcTemplate.queryForObject(sql, Double.class, companyId);
        return avg != null ? avg : 0.0;
    }

    public List<Map<String, Object>> transferVolumeByProduct(String companyId, Instant from, Instant to, String warehouseId) {
        LocalDateTime fromLdt = LocalDateTime.ofInstant(from, ZoneOffset.UTC);
        LocalDateTime toLdt = LocalDateTime.ofInstant(to, ZoneOffset.UTC);
        String sql = "SELECT productId, sum(quantity) AS totalQty FROM transfer_events " +
                     "WHERE companyId = ? AND status = 'SUGGESTED' AND eventTime BETWEEN ? AND ?" +
                     (warehouseId != null ? " AND (fromWarehouseId = ? OR toWarehouseId = ?)" : "") +
                     " GROUP BY productId ORDER BY totalQty DESC";
        return warehouseId != null
                ? clickHouseJdbcTemplate.queryForList(sql, companyId, fromLdt, toLdt, warehouseId, warehouseId)
                : clickHouseJdbcTemplate.queryForList(sql, companyId, fromLdt, toLdt);
    }
}
