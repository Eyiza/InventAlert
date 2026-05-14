package com.inventalert.analyticsService.repository;

import com.inventalert.analyticsService.dto.event.StockMovementEvent;
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
public class StockMovementEventRepository {

    private final JdbcTemplate clickHouseJdbcTemplate;

    public boolean existsByEventId(String eventId) {
        Integer count = clickHouseJdbcTemplate.queryForObject(
                "SELECT count() FROM stock_movement_events WHERE eventId = ?",
                Integer.class, eventId);
        return count != null && count > 0;
    }

    public void insert(StockMovementEvent e, Instant eventTime) {
        clickHouseJdbcTemplate.update(
                "INSERT INTO stock_movement_events " +
                "(eventId, companyId, movementId, productId, warehouseId, movementType, quantity, eventTime) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                e.eventId(), e.companyId(), e.movementId(), e.productId(),
                e.warehouseId(), e.type(), e.quantity(), LocalDateTime.ofInstant(eventTime, ZoneOffset.UTC));
    }

    public long countAll(String companyId, String warehouseId) {
        String sql = "SELECT count() FROM stock_movement_events WHERE companyId = ?" +
                     (warehouseId != null ? " AND warehouseId = ?" : "");
        Long count = warehouseId != null
                ? clickHouseJdbcTemplate.queryForObject(sql, Long.class, companyId, warehouseId)
                : clickHouseJdbcTemplate.queryForObject(sql, Long.class, companyId);
        return count != null ? count : 0L;
    }

    public long countByMovementType(String companyId, String movementType, String warehouseId) {
        String sql = "SELECT count() FROM stock_movement_events WHERE companyId = ? AND movementType = ?" +
                     (warehouseId != null ? " AND warehouseId = ?" : "");
        Long count = warehouseId != null
                ? clickHouseJdbcTemplate.queryForObject(sql, Long.class, companyId, movementType, warehouseId)
                : clickHouseJdbcTemplate.queryForObject(sql, Long.class, companyId, movementType);
        return count != null ? count : 0L;
    }

    public List<Map<String, Object>> topMovingProducts(String companyId, String movementType, int limit, String warehouseId) {
        String sql = "SELECT productId, sum(quantity) AS totalQty " +
                     "FROM stock_movement_events " +
                     "WHERE companyId = ? AND movementType = ?" +
                     (warehouseId != null ? " AND warehouseId = ?" : "") +
                     " GROUP BY productId ORDER BY totalQty DESC LIMIT ?";
        return warehouseId != null
                ? clickHouseJdbcTemplate.queryForList(sql, companyId, movementType, warehouseId, limit)
                : clickHouseJdbcTemplate.queryForList(sql, companyId, movementType, limit);
    }

    public List<Map<String, Object>> movementTrendByDay(String companyId, Instant from, Instant to, String warehouseId) {
        LocalDateTime fromLdt = LocalDateTime.ofInstant(from, ZoneOffset.UTC);
        LocalDateTime toLdt = LocalDateTime.ofInstant(to, ZoneOffset.UTC);
        String sql = "SELECT toDate(eventTime) AS day, movementType, sum(quantity) AS total " +
                     "FROM stock_movement_events " +
                     "WHERE companyId = ? AND eventTime BETWEEN ? AND ?" +
                     (warehouseId != null ? " AND warehouseId = ?" : "") +
                     " GROUP BY day, movementType ORDER BY day";
        return warehouseId != null
                ? clickHouseJdbcTemplate.queryForList(sql, companyId, fromLdt, toLdt, warehouseId)
                : clickHouseJdbcTemplate.queryForList(sql, companyId, fromLdt, toLdt);
    }

    public List<Map<String, Object>> movementTrendByWarehouse(String companyId, Instant from, Instant to, String warehouseId) {
        LocalDateTime fromLdt = LocalDateTime.ofInstant(from, ZoneOffset.UTC);
        LocalDateTime toLdt = LocalDateTime.ofInstant(to, ZoneOffset.UTC);
        String sql = "SELECT warehouseId, movementType, sum(quantity) AS total " +
                     "FROM stock_movement_events " +
                     "WHERE companyId = ? AND eventTime BETWEEN ? AND ?" +
                     (warehouseId != null ? " AND warehouseId = ?" : "") +
                     " GROUP BY warehouseId, movementType ORDER BY total DESC";
        return warehouseId != null
                ? clickHouseJdbcTemplate.queryForList(sql, companyId, fromLdt, toLdt, warehouseId)
                : clickHouseJdbcTemplate.queryForList(sql, companyId, fromLdt, toLdt);
    }
}
