package com.inventalert.analyticsService.integration;

import com.inventalert.analyticsService.dto.event.TransferEvent;
import com.inventalert.analyticsService.repository.TransferEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransferEventRepositoryIT extends ClickHouseIntegrationTest {

    @Autowired
    TransferEventRepository repo;

    @Test
    void insert_suggestedWithDistance_thenExists() {
        String eventId = "tr-evt-" + System.nanoTime();
        String companyId = "co-tr-" + System.nanoTime();
        TransferEvent event = new TransferEvent(
                eventId, companyId, "sug-1", "prod-1", "wh-a", "wh-b",
                50, 12.5, "SUGGESTED", "2025-05-01T11:00:00Z");

        repo.insert(event, Instant.parse("2025-05-01T11:00:00Z"));

        assertThat(repo.existsByEventId(eventId)).isTrue();
    }

    @Test
    void insert_approvedWithNullDistance_doesNotThrow() {
        String eventId = "tr-approved-" + System.nanoTime();
        String companyId = "co-tr2-" + System.nanoTime();
        TransferEvent event = new TransferEvent(
                eventId, companyId, "sug-2", "prod-2", "wh-a", "wh-b",
                null, null, "APPROVED", "2025-05-02T11:00:00Z");

        repo.insert(event, Instant.parse("2025-05-02T11:00:00Z"));

        assertThat(repo.existsByEventId(eventId)).isTrue();
    }

    @Test
    void transferCountByStatus_afterInsertAllStatuses_returnsAllFour() {
        String companyId = "co-status-" + System.nanoTime();
        Instant now = Instant.now();

        repo.insert(new TransferEvent("s1-" + now.toEpochMilli(), companyId, "sug-s1", "p1", "wh-a", "wh-b",
                10, 5.0, "SUGGESTED", "2025-05-01T00:00:00Z"), Instant.parse("2025-05-01T00:00:00Z"));
        repo.insert(new TransferEvent("s2-" + now.toEpochMilli(), companyId, "sug-s2", "p1", "wh-a", "wh-b",
                null, null, "APPROVED", "2025-05-02T00:00:00Z"), Instant.parse("2025-05-02T00:00:00Z"));
        repo.insert(new TransferEvent("s3-" + now.toEpochMilli(), companyId, "sug-s3", "p1", "wh-a", "wh-b",
                null, null, "REJECTED", "2025-05-03T00:00:00Z"), Instant.parse("2025-05-03T00:00:00Z"));
        repo.insert(new TransferEvent("s4-" + now.toEpochMilli(), companyId, "sug-s4", "p1", "wh-a", "wh-b",
                null, null, "ACCEPTED", "2025-05-04T00:00:00Z"), Instant.parse("2025-05-04T00:00:00Z"));

        List<Map<String, Object>> statusCounts = repo.transferCountByStatus(companyId, null);
        assertThat(statusCounts).hasSize(4);
        assertThat(statusCounts).extracting(m -> m.get("status"))
                .containsExactlyInAnyOrder("SUGGESTED", "APPROVED", "REJECTED", "ACCEPTED");
    }

    @Test
    void avgDistanceKm_returnsPositiveValue() {
        String companyId = "co-dist-" + System.nanoTime();
        repo.insert(new TransferEvent("d1-" + System.nanoTime(), companyId, "sug-d1", "p", "wa", "wb",
                10, 20.0, "SUGGESTED", "2025-05-01T00:00:00Z"), Instant.parse("2025-05-01T00:00:00Z"));
        repo.insert(new TransferEvent("d2-" + System.nanoTime(), companyId, "sug-d2", "p", "wa", "wb",
                10, 40.0, "SUGGESTED", "2025-05-02T00:00:00Z"), Instant.parse("2025-05-02T00:00:00Z"));

        double avg = repo.avgDistanceKm(companyId, null);
        assertThat(avg).isEqualTo(30.0);
    }
}
