package com.inventalert.inventoryService.service.impl;

import com.inventalert.inventoryService.dto.response.TransferSuggestionResponse;
import com.inventalert.inventoryService.exception.*;
import com.inventalert.inventoryService.kafka.TransferEventProducer;
import com.inventalert.inventoryService.model.*;
import com.inventalert.inventoryService.repository.*;
import com.inventalert.inventoryService.service.GoogleMapsService;
import com.inventalert.inventoryService.service.RestockAlertService;
import com.inventalert.inventoryService.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferSuggestionRepository transferRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository stockMovementRepository;
    private final RestockAlertService restockAlertService;
    private final GoogleMapsService googleMapsService;
    private final TransferEventProducer eventProducer;

    @Override
    @Transactional
    public void createSuggestion(String productId, String deficitWarehouseId,
                                  List<StockLevel> candidates, int shortage, String companyId) {
        Warehouse toWarehouse = warehouseRepository.findByIdAndIsActiveTrue(deficitWarehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException(deficitWarehouseId));

        StockLevel bestCandidate = null;
        double minDistance = Double.MAX_VALUE;
        DistanceSource distanceSource = DistanceSource.GOOGLE_MAPS;

        for (StockLevel candidate : candidates) {
            Warehouse fromWarehouse = warehouseRepository
                    .findByIdAndIsActiveTrue(candidate.getWarehouseId()).orElse(null);
            if (fromWarehouse == null) continue;

            Double distance = googleMapsService.getDrivingDistanceKm(
                    fromWarehouse.getId(), fromWarehouse.getLatitude(), fromWarehouse.getLongitude(),
                    toWarehouse.getId(), toWarehouse.getLatitude(), toWarehouse.getLongitude());

            if (distance < minDistance) {
                minDistance = distance;
                bestCandidate = candidate;
            }
        }

        if (bestCandidate == null) return;

        TransferSuggestion suggestion = TransferSuggestion.builder()
                .productId(productId)
                .fromWarehouseId(bestCandidate.getWarehouseId())
                .toWarehouseId(deficitWarehouseId)
                .quantity(shortage)
                .distanceKm(BigDecimal.valueOf(minDistance))
                .distanceSource(distanceSource)
                .status(TransferStatus.SUGGESTED)
                .build();

        TransferSuggestion saved = transferRepository.save(suggestion);
        eventProducer.publishTransferSuggestionCreated(
                companyId, saved.getId(), bestCandidate.getWarehouseId(),
                deficitWarehouseId, productId, shortage, minDistance);
    }

    @Override
    public List<TransferSuggestionResponse> list(String role, String warehouseId) {
        if ("WAREHOUSE_STAFF".equals(role)) {
            return transferRepository.findByFromWarehouseIdOrToWarehouseId(warehouseId, warehouseId)
                    .stream().map(this::toResponse).toList();
        }
        return transferRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void approve(String id, String managerId, String companyId) {
        TransferSuggestion suggestion = findOrThrow(id);
        if (suggestion.getStatus() != TransferStatus.SUGGESTED) {
            throw new InvalidStateTransitionException("TransferSuggestion", suggestion.getStatus().name(), "approve");
        }
        suggestion.setStatus(TransferStatus.APPROVED);
        suggestion.setApprovedBy(managerId);
        transferRepository.save(suggestion);
        eventProducer.publishTransferApproved(companyId, id, managerId);
    }

    @Override
    @Transactional
    public void reject(String id, String managerId, String companyId) {
        TransferSuggestion suggestion = findOrThrow(id);
        if (suggestion.getStatus() != TransferStatus.SUGGESTED) {
            throw new InvalidStateTransitionException("TransferSuggestion", suggestion.getStatus().name(), "reject");
        }
        suggestion.setStatus(TransferStatus.REJECTED);
        transferRepository.save(suggestion);

        RestockAlert alert = restockAlertService.createAlert(
                suggestion.getProductId(), suggestion.getToWarehouseId(), 0, 0, companyId);
        String alertId = alert != null ? alert.getId() : null;
        eventProducer.publishTransferRejected(companyId, id, managerId, alertId);
    }

    @Override
    @Transactional
    public void dispatch(String id, String staffId, String staffWarehouseId, String companyId) {
        TransferSuggestion suggestion = findOrThrow(id);
        if (suggestion.getStatus() != TransferStatus.APPROVED) {
            throw new InvalidStateTransitionException("TransferSuggestion", suggestion.getStatus().name(), "dispatch");
        }
        if (!suggestion.getFromWarehouseId().equals(staffWarehouseId)) {
            throw new WarehouseNotAssignedException(suggestion.getFromWarehouseId());
        }

        StockLevel fromStock = stockLevelRepository
                .findByProductIdAndWarehouseId(suggestion.getProductId(), suggestion.getFromWarehouseId())
                .orElseThrow(() -> new StockLevelNotFoundException(suggestion.getProductId(), suggestion.getFromWarehouseId()));

        fromStock.setCurrentStock(fromStock.getCurrentStock() - suggestion.getQuantity());
        stockLevelRepository.save(fromStock);

        stockMovementRepository.save(StockMovement.builder()
                .productId(suggestion.getProductId())
                .warehouseId(suggestion.getFromWarehouseId())
                .type(MovementType.TRANSFER_OUT)
                .quantity(suggestion.getQuantity())
                .referenceId(id)
                .createdBy(staffId)
                .build());

        suggestion.setStatus(TransferStatus.IN_TRANSIT);
        transferRepository.save(suggestion);
    }

    @Override
    @Transactional
    public void accept(String id, String staffId, String staffWarehouseId, String companyId) {
        TransferSuggestion suggestion = findOrThrow(id);
        if (suggestion.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new InvalidStateTransitionException("TransferSuggestion", suggestion.getStatus().name(), "accept");
        }
        if (!suggestion.getToWarehouseId().equals(staffWarehouseId)) {
            throw new WarehouseNotAssignedException(suggestion.getToWarehouseId());
        }

        StockLevel toStock = stockLevelRepository
                .findByProductIdAndWarehouseId(suggestion.getProductId(), suggestion.getToWarehouseId())
                .orElseThrow(() -> new StockLevelNotFoundException(suggestion.getProductId(), suggestion.getToWarehouseId()));

        toStock.setCurrentStock(toStock.getCurrentStock() + suggestion.getQuantity());
        stockLevelRepository.save(toStock);

        stockMovementRepository.save(StockMovement.builder()
                .productId(suggestion.getProductId())
                .warehouseId(suggestion.getToWarehouseId())
                .type(MovementType.TRANSFER_IN)
                .quantity(suggestion.getQuantity())
                .referenceId(id)
                .createdBy(staffId)
                .build());

        suggestion.setStatus(TransferStatus.COMPLETED);
        transferRepository.save(suggestion);
        eventProducer.publishTransferAccepted(companyId, id, staffId);
    }

    @Override
    @Transactional
    public void rejectDelivery(String id, String staffId, String staffWarehouseId, String companyId) {
        TransferSuggestion suggestion = findOrThrow(id);
        if (suggestion.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new InvalidStateTransitionException("TransferSuggestion", suggestion.getStatus().name(), "reject delivery");
        }
        if (!suggestion.getToWarehouseId().equals(staffWarehouseId)) {
            throw new WarehouseNotAssignedException(suggestion.getToWarehouseId());
        }
        suggestion.setStatus(TransferStatus.DELIVERY_REJECTED);
        transferRepository.save(suggestion);

        RestockAlert alert = restockAlertService.createAlert(
                suggestion.getProductId(), suggestion.getToWarehouseId(), 0, 0, companyId);
        String alertId = alert != null ? alert.getId() : null;
        eventProducer.publishTransferRejected(companyId, id, staffId, alertId);
    }

    private TransferSuggestion findOrThrow(String id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new TransferNotFoundException(id));
    }

    private TransferSuggestionResponse toResponse(TransferSuggestion t) {
        return TransferSuggestionResponse.builder()
                .id(t.getId()).productId(t.getProductId())
                .fromWarehouseId(t.getFromWarehouseId()).toWarehouseId(t.getToWarehouseId())
                .quantity(t.getQuantity()).distanceKm(t.getDistanceKm())
                .distanceSource(t.getDistanceSource() != null ? t.getDistanceSource().name() : null)
                .status(t.getStatus().name()).approvedBy(t.getApprovedBy())
                .createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
                .build();
    }
}
