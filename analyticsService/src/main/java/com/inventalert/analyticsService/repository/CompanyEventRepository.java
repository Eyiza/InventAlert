package com.inventalert.analyticsService.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CompanyEventRepository {

    private final JdbcTemplate clickHouseJdbcTemplate;

    public boolean existsByEventId(String eventId) {
        Integer count = clickHouseJdbcTemplate.queryForObject(
                "SELECT count() FROM company_events WHERE eventId = ?",
                Integer.class, eventId);
        return count != null && count > 0;
    }

    public void insert(String eventId, String companyId, String companyName,
                       String adminEmail, String eventType, Instant eventTime) {
        clickHouseJdbcTemplate.update(
                "INSERT INTO company_events " +
                "(eventId, companyId, companyName, adminEmail, eventType, eventTime) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                eventId, companyId, companyName, adminEmail, eventType,
                Timestamp.from(eventTime));
    }

    public long countByEventType(String eventType) {
        Long count = clickHouseJdbcTemplate.queryForObject(
                "SELECT count() FROM company_events WHERE eventType = ?",
                Long.class, eventType);
        return count != null ? count : 0L;
    }

    public List<Map<String, Object>> countByMonthAndEventType(String eventType, int months) {
        return clickHouseJdbcTemplate.queryForList(
                "SELECT toYYYYMM(eventTime) AS month, count() AS total " +
                "FROM company_events " +
                "WHERE eventType = ? AND eventTime >= now() - INTERVAL ? MONTH " +
                "GROUP BY month ORDER BY month",
                eventType, months);
    }
}
