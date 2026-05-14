package com.inventalert.analyticsService.repository;

import com.inventalert.analyticsService.dto.event.RestockAlertEvent;
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
public class AlertEventRepository {

    private final JdbcTemplate clickHouseJdbcTemplate;

    public boolean existsByEventId(String eventId) {
        Integer count = clickHouseJdbcTemplate.queryForObject(
                "SELECT count() FROM alert_events WHERE eventId = ?",
                Integer.class, eventId);
        return count != null && count > 0;
    }

    public void insert(RestockAlertEvent e, Instant eventTime) {
        clickHouseJdbcTemplate.update(
                "INSERT INTO alert_events " +
                "(eventId, companyId, alertId, productId, warehouseId, stockAtAlert, threshold, eventTime) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                e.eventId(), e.companyId(), e.alertId(), e.productId(),
                e.warehouseId(), e.stockAtAlert(), e.threshold(), LocalDateTime.ofInstant(eventTime, ZoneOffset.UTC));
    }

    public long countAll(String companyId, String warehouseId) {
        String sql = "SELECT count() FROM alert_events WHERE companyId = ?" +
                     (warehouseId != null ? " AND warehouseId = ?" : "");
        Long count = warehouseId != null
                ? clickHouseJdbcTemplate.queryForObject(sql, Long.class, companyId, warehouseId)
                : clickHouseJdbcTemplate.queryForObject(sql, Long.class, companyId);
        return count != null ? count : 0L;
    }

    public List<Map<String, Object>> alertFrequencyByWarehouse(String companyId, Instant from, Instant to, String warehouseId) {
        LocalDateTime fromLdt = LocalDateTime.ofInstant(from, ZoneOffset.UTC);
        LocalDateTime toLdt = LocalDateTime.ofInstant(to, ZoneOffset.UTC);
        String sql = "SELECT warehouseId, count() AS total FROM alert_events " +
                     "WHERE companyId = ? AND eventTime BETWEEN ? AND ?" +
                     (warehouseId != null ? " AND warehouseId = ?" : "") +
                     " GROUP BY warehouseId ORDER BY total DESC";
        return warehouseId != null
                ? clickHouseJdbcTemplate.queryForList(sql, companyId, fromLdt, toLdt, warehouseId)
                : clickHouseJdbcTemplate.queryForList(sql, companyId, fromLdt, toLdt);
    }

    public List<Map<String, Object>> alertCountByMonth(String companyId, String warehouseId) {
        String sql = "SELECT toYYYYMM(eventTime) AS month, count() AS total " +
                     "FROM alert_events WHERE companyId = ?" +
                     (warehouseId != null ? " AND warehouseId = ?" : "") +
                     " GROUP BY month ORDER BY month";
        return warehouseId != null
                ? clickHouseJdbcTemplate.queryForList(sql, companyId, warehouseId)
                : clickHouseJdbcTemplate.queryForList(sql, companyId);
    }

    public List<Map<String, Object>> avgStockAtAlertVsThreshold(String companyId) {
        return clickHouseJdbcTemplate.queryForList(
                "SELECT productId, avg(stockAtAlert) AS avgStock, avg(threshold) AS avgThreshold " +
                "FROM alert_events WHERE companyId = ? " +
                "GROUP BY productId",
                companyId);
    }
}
