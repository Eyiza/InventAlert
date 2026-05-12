package com.inventalert.analyticsService.repository;

import com.inventalert.analyticsService.dto.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class NotificationEventRepository {

    private final JdbcTemplate clickHouseJdbcTemplate;

    public boolean existsByEventId(String eventId) {
        Integer count = clickHouseJdbcTemplate.queryForObject(
                "SELECT count() FROM notification_events WHERE eventId = ?",
                Integer.class, eventId);
        return count != null && count > 0;
    }

    public void insert(NotificationEvent e, Instant eventTime) {
        clickHouseJdbcTemplate.update(
                "INSERT INTO notification_events " +
                "(eventId, companyId, userId, notifType, referenceId, eventTime) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                e.eventId(), e.companyId(), e.userId(), e.type(), e.referenceId(),
                Timestamp.from(eventTime));
    }

    public long countAll(String companyId) {
        Long count = clickHouseJdbcTemplate.queryForObject(
                "SELECT count() FROM notification_events WHERE companyId = ?",
                Long.class, companyId);
        return count != null ? count : 0L;
    }

    public List<Map<String, Object>> notificationBreakdownByType(String companyId, Instant from, Instant to) {
        return clickHouseJdbcTemplate.queryForList(
                "SELECT notifType, count() AS total FROM notification_events " +
                "WHERE companyId = ? AND eventTime BETWEEN ? AND ? " +
                "GROUP BY notifType ORDER BY total DESC",
                companyId, Timestamp.from(from), Timestamp.from(to));
    }

    public List<Map<String, Object>> notificationVolumeByDay(String companyId, Instant from, Instant to) {
        return clickHouseJdbcTemplate.queryForList(
                "SELECT toDate(eventTime) AS day, count() AS total FROM notification_events " +
                "WHERE companyId = ? AND eventTime BETWEEN ? AND ? " +
                "GROUP BY day ORDER BY day",
                companyId, Timestamp.from(from), Timestamp.from(to));
    }

    public List<Map<String, Object>> topNotifiedUsers(String companyId, int limit) {
        return clickHouseJdbcTemplate.queryForList(
                "SELECT userId, count() AS total FROM notification_events " +
                "WHERE companyId = ? GROUP BY userId ORDER BY total DESC LIMIT ?",
                companyId, limit);
    }
}
