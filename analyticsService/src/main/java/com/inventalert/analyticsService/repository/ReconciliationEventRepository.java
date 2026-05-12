package com.inventalert.analyticsService.repository;

import com.inventalert.analyticsService.dto.event.ReconciliationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ReconciliationEventRepository {

    private final JdbcTemplate clickHouseJdbcTemplate;

    public boolean existsByEventId(String eventId) {
        Integer count = clickHouseJdbcTemplate.queryForObject(
                "SELECT count() FROM reconciliation_events WHERE eventId = ?",
                Integer.class, eventId);
        return count != null && count > 0;
    }

    public void insert(ReconciliationEvent e, Instant eventTime) {
        clickHouseJdbcTemplate.update(
                "INSERT INTO reconciliation_events " +
                "(eventId, companyId, reconciliationId, warehouseId, eventTime) " +
                "VALUES (?, ?, ?, ?, ?)",
                e.eventId(), e.companyId(), e.reconciliationId(), e.warehouseId(),
                Timestamp.from(eventTime));
    }

    public List<Map<String, Object>> reconciliationCountByWarehouse(String companyId) {
        return clickHouseJdbcTemplate.queryForList(
                "SELECT warehouseId, count() AS total FROM reconciliation_events " +
                "WHERE companyId = ? GROUP BY warehouseId",
                companyId);
    }

    public List<Map<String, Object>> reconciliationFrequencyByMonth(String companyId) {
        return clickHouseJdbcTemplate.queryForList(
                "SELECT toYYYYMM(eventTime) AS month, count() AS total " +
                "FROM reconciliation_events WHERE companyId = ? " +
                "GROUP BY month ORDER BY month",
                companyId);
    }
}
