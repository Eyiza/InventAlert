package com.inventalert.inventoryService.service.impl;

import com.inventalert.inventoryService.dto.response.RestockAlertResponse;
import com.inventalert.inventoryService.exception.AlertNotFoundException;
import com.inventalert.inventoryService.exception.InvalidStateTransitionException;
import com.inventalert.inventoryService.kafka.AlertEventProducer;
import com.inventalert.inventoryService.model.AlertStatus;
import com.inventalert.inventoryService.model.RestockAlert;
import com.inventalert.inventoryService.repository.RestockAlertRepository;
import com.inventalert.inventoryService.service.RestockAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestockAlertServiceImpl implements RestockAlertService {

    private final RestockAlertRepository alertRepository;
    private final AlertEventProducer alertEventProducer;

    @Override
    @Transactional
    public RestockAlert createAlert(String productId, String warehouseId, int stockAtAlert, int threshold, String companyId) {
        if (alertRepository.existsByProductIdAndWarehouseIdAndStatus(productId, warehouseId, AlertStatus.OPEN)) {
            return null; // idempotent — already open alert exists
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
        return saved;
    }

    @Override
    public List<RestockAlertResponse> list() {
        return alertRepository.findAll().stream().map(this::toResponse).toList();
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
        alert.setStatus(AlertStatus.RESOLVED);
        alertRepository.save(alert);
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
