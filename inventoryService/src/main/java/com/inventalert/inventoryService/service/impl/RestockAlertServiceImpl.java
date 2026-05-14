package com.inventalert.inventoryService.service.impl;

import com.inventalert.inventoryService.dto.response.RestockAlertResponse;
import com.inventalert.inventoryService.exception.AlertNotFoundException;
import com.inventalert.inventoryService.exception.InvalidStateTransitionException;
import com.inventalert.inventoryService.exception.StockNotChangedException;
import com.inventalert.inventoryService.repository.StockLevelRepository;
import com.inventalert.inventoryService.kafka.AlertEventProducer;
import com.inventalert.inventoryService.model.AlertStatus;
import com.inventalert.inventoryService.model.RestockAlert;
import com.inventalert.inventoryService.repository.RestockAlertRepository;
import com.inventalert.inventoryService.service.RestockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestockAlertServiceImpl implements RestockAlertService {

    private final RestockAlertRepository alertRepository;
    private final StockLevelRepository stockLevelRepository;
    private final AlertEventProducer alertEventProducer;
    private final JdbcTemplate jdbcTemplate;

    @Value("${identity.db.name:inventalert_identity}")
    private String identityDbName;

    @Override
    @Transactional
    public RestockAlert createAlert(String productId, String warehouseId, int stockAtAlert, int threshold, String companyId) {
        if (alertRepository.existsByProductIdAndWarehouseIdAndStatus(productId, warehouseId, AlertStatus.OPEN)) {
            return null;
        }
        RestockAlert alert = RestockAlert.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .stockAtAlert(stockAtAlert)
                .threshold(threshold)
                .status(AlertStatus.OPEN)
                .build();
        RestockAlert saved = alertRepository.save(alert);
        alertEventProducer.publishAlertCreated(
                companyId, saved.getId(), productId, warehouseId, companyId, stockAtAlert, threshold);

        notifyProcurementOfficers(companyId, warehouseId, saved.getId(), stockAtAlert, threshold);
        return saved;
    }

    @Override
    public List<RestockAlertResponse> list(AlertStatus status) {
        List<RestockAlert> alerts = (status != null)
                ? alertRepository.findByStatusOrderByCreatedAtDesc(status)
                : alertRepository.findAllByOrderByCreatedAtDesc();
        return alerts.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void autoResolveForProduct(String productId, String warehouseId) {
        List<RestockAlert> active = alertRepository
                .findByProductIdAndWarehouseIdAndStatusNot(productId, warehouseId, AlertStatus.RESOLVED);
        if (active.isEmpty()) return;
        for (RestockAlert alert : active) {
            alert.setStatus(AlertStatus.RESOLVED);
        }
        alertRepository.saveAll(active);
    }

    @Override
    @Transactional
    public void acknowledge(String id, String userId) {
        RestockAlert alert = findOrThrow(id);
        if (alert.getStatus() != AlertStatus.OPEN) {
            throw new InvalidStateTransitionException("RestockAlert", alert.getStatus().name(), "acknowledge");
        }
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alertRepository.save(alert);
    }

    @Override
    @Transactional
    public void markOrderPlaced(String id, String userId) {
        RestockAlert alert = findOrThrow(id);
        if (alert.getStatus() != AlertStatus.ACKNOWLEDGED) {
            throw new InvalidStateTransitionException("RestockAlert", alert.getStatus().name(), "mark order placed");
        }
        alert.setStatus(AlertStatus.ORDER_PLACED);
        alertRepository.save(alert);
    }

    @Override
    @Transactional
    public void resolve(String id, String userId) {
        RestockAlert alert = findOrThrow(id);
        if (alert.getStatus() != AlertStatus.ORDER_PLACED) {
            throw new InvalidStateTransitionException("RestockAlert", alert.getStatus().name(), "resolve");
        }
        int currentStock = stockLevelRepository
                .findByProductIdAndWarehouseId(alert.getProductId(), alert.getWarehouseId())
                .map(sl -> sl.getCurrentStock())
                .orElse(0);
        if (currentStock <= alert.getThreshold()) {
            throw new StockNotChangedException(currentStock, alert.getThreshold());
        }
        alert.setStatus(AlertStatus.RESOLVED);
        alertRepository.save(alert);
    }

    private void notifyProcurementOfficers(String companyId, String warehouseId,
                                             String alertId, int stockAtAlert, int threshold) {
        try {
            String sql = "SELECT u.id, u.email FROM " + identityDbName + ".User u " +
                         "JOIN " + identityDbName + ".WarehouseAssignment wa ON wa.userId = u.id " +
                         "WHERE wa.warehouseId = ? AND wa.companyId = ? " +
                         "AND u.role = 'PROCUREMENT_OFFICER' AND u.isActive = 1";
            List<Map<String, Object>> officers = jdbcTemplate.queryForList(sql, warehouseId, companyId);
            for (Map<String, Object> officer : officers) {
                alertEventProducer.publishNotificationEvent(
                        companyId,
                        (String) officer.get("id"),
                        (String) officer.get("email"),
                        "RESTOCK_ALERT",
                        "Low stock alert: stock has dropped to " + stockAtAlert
                                + " (threshold: " + threshold + "). Immediate restocking may be required.",
                        alertId);
            }
        } catch (Exception e) {
            log.warn("Could not notify procurement officers for alert {}: {}", alertId, e.getMessage());
        }
    }

    private RestockAlert findOrThrow(String id) {
        return alertRepository.findById(id).orElseThrow(() -> new AlertNotFoundException(id));
    }

    private RestockAlertResponse toResponse(RestockAlert a) {
        return RestockAlertResponse.builder()
                .id(a.getId()).productId(a.getProductId()).warehouseId(a.getWarehouseId())
                .stockAtAlert(a.getStockAtAlert()).threshold(a.getThreshold())
                .status(a.getStatus().name()).assignedTo(a.getAssignedTo())
                .createdAt(a.getCreatedAt()).updatedAt(a.getUpdatedAt())
                .build();
    }
}
