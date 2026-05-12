package com.inventalert.analyticsService.service;

import com.inventalert.analyticsService.dto.event.*;
import com.inventalert.analyticsService.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsIngestionService {

    private final CompanyEventRepository companyEventRepo;
    private final StockMovementEventRepository stockMovementRepo;
    private final AlertEventRepository alertRepo;
    private final TransferEventRepository transferRepo;
    private final ReconciliationEventRepository reconciliationRepo;
    private final NotificationEventRepository notificationRepo;

    public void ingestCompanyCreated(CompanyCreatedEvent event) {
        if (companyEventRepo.existsByEventId(event.eventId())) {
            log.debug("Duplicate company.created event {}, skipping", event.eventId());
            return;
        }
        companyEventRepo.insert(event.eventId(), event.companyId(), event.companyName(),
                event.adminEmail(), "CREATED", Instant.parse(event.timestamp()));
    }

    public void ingestCompanyOffboarded(CompanyOffboardedEvent event) {
        if (companyEventRepo.existsByEventId(event.eventId())) return;
        companyEventRepo.insert(event.eventId(), event.companyId(), "", "", "OFFBOARDED",
                Instant.parse(event.timestamp()));
    }

    public void ingestStockMovement(StockMovementEvent event) {
        if (stockMovementRepo.existsByEventId(event.eventId())) return;
        stockMovementRepo.insert(event, Instant.parse(event.timestamp()));
    }

    public void ingestRestockAlert(RestockAlertEvent event) {
        if (alertRepo.existsByEventId(event.eventId())) return;
        alertRepo.insert(event, Instant.parse(event.timestamp()));
    }

    public void ingestTransferEvent(TransferEvent event) {
        if (transferRepo.existsByEventId(event.eventId())) return;
        transferRepo.insert(event, Instant.parse(event.timestamp()));
    }

    public void ingestReconciliation(ReconciliationEvent event) {
        if (reconciliationRepo.existsByEventId(event.eventId())) return;
        reconciliationRepo.insert(event, Instant.parse(event.timestamp()));
    }

    public void ingestNotification(NotificationEvent event) {
        if (notificationRepo.existsByEventId(event.eventId())) return;
        notificationRepo.insert(event, Instant.now());
    }
}
