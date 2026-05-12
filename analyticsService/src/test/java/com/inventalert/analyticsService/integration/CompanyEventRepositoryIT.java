package com.inventalert.analyticsService.integration;

import com.inventalert.analyticsService.repository.CompanyEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyEventRepositoryIT extends ClickHouseIntegrationTest {

    @Autowired
    CompanyEventRepository repo;

    @Test
    void insert_thenExistsByEventId_returnsTrue() {
        String eventId = "it-evt-" + System.nanoTime();
        repo.insert(eventId, "co-it-1", "IT Corp", "it@corp.com", "CREATED", Instant.now());

        assertThat(repo.existsByEventId(eventId)).isTrue();
    }

    @Test
    void existsByEventId_unknownId_returnsFalse() {
        assertThat(repo.existsByEventId("nonexistent-evt")).isFalse();
    }

    @Test
    void insert_duplicate_doesNotThrow_existsReturnsTrue() {
        String eventId = "dup-evt-" + System.nanoTime();
        repo.insert(eventId, "co-it-2", "Corp B", "b@corp.com", "CREATED", Instant.now());
        // Second insert (idempotency is app-level, but raw insert should not throw)
        repo.insert(eventId, "co-it-2", "Corp B", "b@corp.com", "CREATED", Instant.now());

        assertThat(repo.existsByEventId(eventId)).isTrue();
    }

    @Test
    void countByEventType_afterInsert_returnsCorrectCount() {
        String eventId = "count-evt-" + System.nanoTime();
        repo.insert(eventId, "co-count-1", "Count Corp", "c@corp.com", "CREATED", Instant.now());

        long created = repo.countByEventType("CREATED");
        assertThat(created).isPositive();
    }

    @Test
    void countByMonthAndEventType_returnsMonthlyRows() {
        String eventId = "month-evt-" + System.nanoTime();
        repo.insert(eventId, "co-month-1", "Month Corp", "m@corp.com", "CREATED", Instant.now());

        List<Map<String, Object>> result = repo.countByMonthAndEventType("CREATED", 1);
        assertThat(result).isNotEmpty();
    }
}
