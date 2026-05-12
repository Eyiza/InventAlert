package com.inventalert.analyticsService.integration;

import com.inventalert.analyticsService.dto.event.NotificationEvent;
import com.inventalert.analyticsService.repository.NotificationEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEventRepositoryIT extends ClickHouseIntegrationTest {

    @Autowired
    NotificationEventRepository repo;

    @Test
    void insert_thenExistsByEventId_returnsTrue() {
        String eventId = "notif-evt-" + System.nanoTime();
        NotificationEvent event = new NotificationEvent(
                eventId, "co-notif-1", "user-1", "u@co.com",
                "RESTOCK_ALERT", "Low stock", "alert-1");

        repo.insert(event, Instant.now());

        assertThat(repo.existsByEventId(eventId)).isTrue();
    }

    @Test
    void existsByEventId_unknownId_returnsFalse() {
        assertThat(repo.existsByEventId("notif-nonexistent")).isFalse();
    }

    @Test
    void countAll_afterInsert_isPositive() {
        String companyId = "co-notif-count-" + System.nanoTime();
        NotificationEvent event = new NotificationEvent(
                "n-count-" + System.nanoTime(), companyId, "user-2", "u@co.com",
                "TRANSFER_SUGGESTION", "Transfer ready", "sug-1");

        repo.insert(event, Instant.now());

        assertThat(repo.countAll(companyId)).isPositive();
    }

    @Test
    void topNotifiedUsers_returnsUserWithHighestCount() {
        String companyId = "co-top-notif-" + System.nanoTime();
        Instant now = Instant.now();

        for (int i = 0; i < 3; i++) {
            repo.insert(new NotificationEvent(
                    "n-top-" + i + "-" + now.toEpochMilli(), companyId, "heavy-user",
                    "h@co.com", "RESTOCK_ALERT", "msg", "ref-" + i), now);
        }
        repo.insert(new NotificationEvent(
                "n-once-" + now.toEpochMilli(), companyId, "light-user",
                "l@co.com", "RESTOCK_ALERT", "msg", "ref-x"), now);

        List<Map<String, Object>> top = repo.topNotifiedUsers(companyId, 5);
        assertThat(top).isNotEmpty();
        assertThat(top.get(0).get("userId")).isEqualTo("heavy-user");
    }

    @Test
    void notificationBreakdownByType_groupsByType() {
        String companyId = "co-breakdown-" + System.nanoTime();
        Instant now = Instant.now();

        repo.insert(new NotificationEvent("nb-1-" + now.toEpochMilli(), companyId, "u1",
                "u@co.com", "RESTOCK_ALERT", "msg", "r1"), now);
        repo.insert(new NotificationEvent("nb-2-" + now.toEpochMilli(), companyId, "u2",
                "u@co.com", "TRANSFER_SUGGESTION", "msg", "r2"), now);

        Instant from = now.minusSeconds(60);
        Instant to = now.plusSeconds(60);
        List<Map<String, Object>> breakdown = repo.notificationBreakdownByType(companyId, from, to);
        assertThat(breakdown).hasSize(2);
    }
}
