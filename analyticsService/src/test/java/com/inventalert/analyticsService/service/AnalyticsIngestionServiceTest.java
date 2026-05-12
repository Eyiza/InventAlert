package com.inventalert.analyticsService.service;

import com.inventalert.analyticsService.dto.event.*;
import com.inventalert.analyticsService.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsIngestionServiceTest {

    @Mock CompanyEventRepository companyEventRepo;
    @Mock StockMovementEventRepository stockMovementRepo;
    @Mock AlertEventRepository alertRepo;
    @Mock TransferEventRepository transferRepo;
    @Mock ReconciliationEventRepository reconciliationRepo;
    @Mock NotificationEventRepository notificationRepo;

    @InjectMocks
    AnalyticsIngestionService service;

    // ── CompanyCreated ────────────────────────────────────────────────────────

    @Test
    void ingestCompanyCreated_newEvent_insertsOnce() {
        CompanyCreatedEvent event = new CompanyCreatedEvent(
                "evt-1", "co-1", "Acme", "admin@acme.com", "2025-01-15T10:00:00Z");
        when(companyEventRepo.existsByEventId("evt-1")).thenReturn(false);

        service.ingestCompanyCreated(event);

        verify(companyEventRepo).insert("evt-1", "co-1", "Acme", "admin@acme.com",
                "CREATED", Instant.parse("2025-01-15T10:00:00Z"));
    }

    @Test
    void ingestCompanyCreated_duplicateEventId_skipsInsert() {
        CompanyCreatedEvent event = new CompanyCreatedEvent(
                "evt-dup", "co-1", "Acme", "admin@acme.com", "2025-01-15T10:00:00Z");
        when(companyEventRepo.existsByEventId("evt-dup")).thenReturn(true);

        service.ingestCompanyCreated(event);

        verify(companyEventRepo, never()).insert(any(), any(), any(), any(), any(), any());
    }

    @Test
    void ingestCompanyOffboarded_newEvent_insertsWithOffboardedType() {
        CompanyOffboardedEvent event = new CompanyOffboardedEvent(
                "evt-2", "co-1", "2025-06-01T08:00:00Z");
        when(companyEventRepo.existsByEventId("evt-2")).thenReturn(false);

        service.ingestCompanyOffboarded(event);

        verify(companyEventRepo).insert(eq("evt-2"), eq("co-1"), eq(""), eq(""),
                eq("OFFBOARDED"), eq(Instant.parse("2025-06-01T08:00:00Z")));
    }

    @Test
    void ingestCompanyOffboarded_duplicateEventId_skipsInsert() {
        CompanyOffboardedEvent event = new CompanyOffboardedEvent(
                "evt-dup2", "co-1", "2025-06-01T08:00:00Z");
        when(companyEventRepo.existsByEventId("evt-dup2")).thenReturn(true);

        service.ingestCompanyOffboarded(event);

        verify(companyEventRepo, never()).insert(any(), any(), any(), any(), any(), any());
    }

    // ── StockMovement ─────────────────────────────────────────────────────────

    @Test
    void ingestStockMovement_newEvent_insertsOnce() {
        StockMovementEvent event = new StockMovementEvent(
                "evt-3", "co-1", "mov-1", "prod-1", "wh-1", "INTAKE", 100, "2025-03-10T12:00:00Z");
        when(stockMovementRepo.existsByEventId("evt-3")).thenReturn(false);

        service.ingestStockMovement(event);

        verify(stockMovementRepo).insert(event, Instant.parse("2025-03-10T12:00:00Z"));
    }

    @Test
    void ingestStockMovement_duplicateEventId_skipsInsert() {
        StockMovementEvent event = new StockMovementEvent(
                "evt-dup3", "co-1", "mov-1", "prod-1", "wh-1", "INTAKE", 100, "2025-03-10T12:00:00Z");
        when(stockMovementRepo.existsByEventId("evt-dup3")).thenReturn(true);

        service.ingestStockMovement(event);

        verify(stockMovementRepo, never()).insert(any(), any());
    }

    // ── RestockAlert ──────────────────────────────────────────────────────────

    @Test
    void ingestRestockAlert_newEvent_insertsOnce() {
        RestockAlertEvent event = new RestockAlertEvent(
                "evt-4", "co-1", "alert-1", "prod-1", "wh-1", 5, 20, "2025-04-01T09:00:00Z");
        when(alertRepo.existsByEventId("evt-4")).thenReturn(false);

        service.ingestRestockAlert(event);

        verify(alertRepo).insert(event, Instant.parse("2025-04-01T09:00:00Z"));
    }

    @Test
    void ingestRestockAlert_duplicate_skipsInsert() {
        RestockAlertEvent event = new RestockAlertEvent(
                "evt-dup4", "co-1", "alert-1", "prod-1", "wh-1", 5, 20, "2025-04-01T09:00:00Z");
        when(alertRepo.existsByEventId("evt-dup4")).thenReturn(true);

        service.ingestRestockAlert(event);

        verify(alertRepo, never()).insert(any(), any());
    }

    // ── TransferEvent ─────────────────────────────────────────────────────────

    @Test
    void ingestTransferEvent_newEvent_insertsOnce() {
        TransferEvent event = new TransferEvent(
                "evt-5", "co-1", "sug-1", "prod-1", "wh-a", "wh-b",
                50, 12.5, "SUGGESTED", "2025-05-01T11:00:00Z");
        when(transferRepo.existsByEventId("evt-5")).thenReturn(false);

        service.ingestTransferEvent(event);

        verify(transferRepo).insert(event, Instant.parse("2025-05-01T11:00:00Z"));
    }

    @Test
    void ingestTransferEvent_duplicate_skipsInsert() {
        TransferEvent event = new TransferEvent(
                "evt-dup5", "co-1", "sug-1", "prod-1", "wh-a", "wh-b",
                50, null, "APPROVED", "2025-05-01T11:00:00Z");
        when(transferRepo.existsByEventId("evt-dup5")).thenReturn(true);

        service.ingestTransferEvent(event);

        verify(transferRepo, never()).insert(any(), any());
    }

    // ── ReconciliationEvent ───────────────────────────────────────────────────

    @Test
    void ingestReconciliation_newEvent_insertsOnce() {
        ReconciliationEvent event = new ReconciliationEvent(
                "evt-6", "co-1", "rec-1", "wh-1", "2025-07-10T14:00:00Z");
        when(reconciliationRepo.existsByEventId("evt-6")).thenReturn(false);

        service.ingestReconciliation(event);

        verify(reconciliationRepo).insert(event, Instant.parse("2025-07-10T14:00:00Z"));
    }

    // ── NotificationEvent ─────────────────────────────────────────────────────

    @Test
    void ingestNotification_newEvent_usesIngestTimeAsEventTime() {
        NotificationEvent event = new NotificationEvent(
                "evt-7", "co-1", "user-1", "u@co.com", "RESTOCK_ALERT", "Low stock", "alert-1");
        when(notificationRepo.existsByEventId("evt-7")).thenReturn(false);

        service.ingestNotification(event);

        verify(notificationRepo).insert(eq(event), any(Instant.class));
    }

    @Test
    void ingestNotification_duplicate_skipsInsert() {
        NotificationEvent event = new NotificationEvent(
                "evt-dup7", "co-1", "user-1", "u@co.com", "RESTOCK_ALERT", "Low stock", "alert-1");
        when(notificationRepo.existsByEventId("evt-dup7")).thenReturn(true);

        service.ingestNotification(event);

        verify(notificationRepo, never()).insert(any(), any());
    }
}
