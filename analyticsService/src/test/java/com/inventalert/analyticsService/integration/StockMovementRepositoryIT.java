package com.inventalert.analyticsService.integration;

import com.inventalert.analyticsService.dto.event.StockMovementEvent;
import com.inventalert.analyticsService.repository.StockMovementEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StockMovementRepositoryIT extends ClickHouseIntegrationTest {

    @Autowired
    StockMovementEventRepository repo;

    @Test
    void insert_thenExistsByEventId_returnsTrue() {
        String eventId = "sm-evt-" + System.nanoTime();
        StockMovementEvent event = new StockMovementEvent(
                eventId, "co-sm-1", "mov-1", "prod-1", "wh-1", "INTAKE", 100,
                "2025-03-10T12:00:00Z");

        repo.insert(event, Instant.parse("2025-03-10T12:00:00Z"));

        assertThat(repo.existsByEventId(eventId)).isTrue();
    }

    @Test
    void existsByEventId_unknownId_returnsFalse() {
        assertThat(repo.existsByEventId("sm-nonexistent")).isFalse();
    }

    @Test
    void topMovingProducts_afterInsert_returnsProduct() {
        String eventId = "top-evt-" + System.nanoTime();
        String companyId = "co-top-" + System.nanoTime();
        StockMovementEvent event = new StockMovementEvent(
                eventId, companyId, "mov-top", "prod-top", "wh-1", "OUTBOUND_SALE", 200,
                "2025-03-10T12:00:00Z");

        repo.insert(event, Instant.parse("2025-03-10T12:00:00Z"));

        List<Map<String, Object>> top = repo.topMovingProducts(companyId, "OUTBOUND_SALE", 5);
        assertThat(top).isNotEmpty();
        assertThat(top.get(0).get("productId")).isEqualTo("prod-top");
    }

    @Test
    void movementTrendByDay_returnsRowsInRange() {
        String eventId = "trend-evt-" + System.nanoTime();
        String companyId = "co-trend-" + System.nanoTime();
        StockMovementEvent event = new StockMovementEvent(
                eventId, companyId, "mov-trend", "prod-2", "wh-2", "INTAKE", 50,
                "2025-03-10T12:00:00Z");

        repo.insert(event, Instant.parse("2025-03-10T12:00:00Z"));

        Instant from = Instant.parse("2025-03-01T00:00:00Z");
        Instant to = Instant.parse("2025-04-01T00:00:00Z");
        List<Map<String, Object>> trend = repo.movementTrendByDay(companyId, from, to);
        assertThat(trend).isNotEmpty();
    }
}
