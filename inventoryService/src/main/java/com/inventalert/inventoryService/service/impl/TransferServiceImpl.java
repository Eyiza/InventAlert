package com.inventalert.inventoryService.service.impl;

import com.inventalert.inventoryService.dto.request.StaffInitiateTransferRequest;
import com.inventalert.inventoryService.dto.response.TransferSuggestionResponse;
import com.inventalert.inventoryService.exception.*;
import com.inventalert.inventoryService.kafka.AlertEventProducer;
import com.inventalert.inventoryService.kafka.TransferEventProducer;
import com.inventalert.inventoryService.model.*;
import com.inventalert.inventoryService.repository.*;
import com.inventalert.inventoryService.service.DistanceResult;
import com.inventalert.inventoryService.service.GoogleMapsService;
import com.inventalert.inventoryService.service.RestockAlertService;
import com.inventalert.inventoryService.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
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
    private final AlertEventProducer alertEventProducer;
    private final JdbcTemplate jdbcTemplate;

    @Value("${transfer.max.distance.km:150}")
    private double maxTransferDistanceKm;

    @Value("${identity.db.name:inventalert_identity}")
    private String identityDbName;

    @Override
    @Transactional
    public void createSuggestion(String productId, String deficitWarehouseId,
                                  List<StockLevel> candidates, int shortage, String companyId) {
        Warehouse toWarehouse = warehouseRepository.findByIdAndIsActiveTrue(deficitWarehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException(deficitWarehouseId));

        StockLevel bestCandidate = null;
        double minDistance = Double.MAX_VALUE;
        DistanceSource bestDistanceSource = DistanceSource.GOOGLE_MAPS;

        for (StockLevel candidate : candidates) {
            Warehouse fromWarehouse = warehouseRepository
                    .findByIdAndIsActiveTrue(candidate.getWarehouseId()).orElse(null);
            if (fromWarehouse == null) continue;

            DistanceResult result = googleMapsService.getDrivingDistanceKm(
                    fromWarehouse.getId(), fromWarehouse.getLatitude(), fromWarehouse.getLongitude(),
                    toWarehouse.getId(), toWarehouse.getLatitude(), toWarehouse.getLongitude());

            if (result.km() < minDistance) {
                minDistance = result.km();
                bestCandidate = candidate;
                bestDistanceSource = result.source();
            }
        }

        if (bestCandidate == null || minDistance > maxTransferDistanceKm) return;

        TransferSuggestion suggestion = TransferSuggestion.builder()
                .productId(productId)
                .fromWarehouseId(bestCandidate.getWarehouseId())
                .toWarehouseId(deficitWarehouseId)
                .quantity(shortage)
                .distanceKm(BigDecimal.valueOf(minDistance))
                .distanceSource(bestDistanceSource)
                .status(TransferStatus.SUGGESTED)
                .build();

        TransferSuggestion saved = transferRepository.save(suggestion);
        eventProducer.publishTransferSuggestionCreated(
                companyId, saved.getId(), bestCandidate.getWarehouseId(),
                deficitWarehouseId, productId, shortage, minDistance);

        notifySourceManager(companyId, bestCandidate.getWarehouseId(), saved.getId(), shortage);
    }

    private void notifySourceManager(String companyId, String fromWarehouseId,
                                      String suggestionId, int quantity) {
        try {
            String sql = "SELECT u.id, u.email FROM " + identityDbName + ".User u " +
                         "JOIN " + identityDbName + ".WarehouseAssignment wa ON wa.userId = u.id " +
                         "WHERE wa.warehouseId = ? AND wa.companyId = ? " +
                         "AND u.role = 'MANAGER' AND u.isActive = 1";
            List<Map<String, Object>> managers = jdbcTemplate.queryForList(sql, fromWarehouseId, companyId);
            for (Map<String, Object> manager : managers) {
                alertEventProducer.publishNotificationEvent(
                        companyId,
                        (String) manager.get("id"),
                        (String) manager.get("email"),
                        "TRANSFER_SUGGESTION",
                        "Transfer suggestion: " + quantity + " unit(s) requested from your warehouse. "
                                + "Please review and approve or reject.",
                        suggestionId);
            }
        } catch (Exception e) {
            log.warn("Could not notify source manager for transfer suggestion {}: {}", suggestionId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public TransferSuggestionResponse initiateByStaff(StaffInitiateTransferRequest request,
                                                      String staffId, String companyId) {
        Warehouse fromWarehouse = warehouseRepository.findByIdAndIsActiveTrue(request.getFromWarehouseId())
                .orElseThrow(() -> new WarehouseNotFoundException(request.getFromWarehouseId()));
        Warehouse toWarehouse = warehouseRepository.findByIdAndIsActiveTrue(request.getToWarehouseId())
                .orElseThrow(() -> new WarehouseNotFoundException(request.getToWarehouseId()));

        DistanceResult distanceResult = googleMapsService.getDrivingDistanceKm(
                fromWarehouse.getId(), fromWarehouse.getLatitude(), fromWarehouse.getLongitude(),
                toWarehouse.getId(), toWarehouse.getLatitude(), toWarehouse.getLongitude());

        TransferSuggestion suggestion = TransferSuggestion.builder()
                .productId(request.getProductId())
                .fromWarehouseId(request.getFromWarehouseId())
                .toWarehouseId(request.getToWarehouseId())
                .quantity(request.getQuantity())
                .distanceKm(BigDecimal.valueOf(distanceResult.km()))
                .distanceSource(distanceResult.source())
                .status(TransferStatus.SUGGESTED)
                .build();

        TransferSuggestion saved = transferRepository.save(suggestion);
        eventProducer.publishTransferSuggestionCreated(
                companyId, saved.getId(), request.getFromWarehouseId(),
                request.getToWarehouseId(), request.getProductId(),
                request.getQuantity(), distanceResult.km());
        return toResponse(saved);
    }

    @Override
    public Page<TransferSuggestionResponse> list(String role, String warehouseId, Pageable pageable) {
        if ("WAREHOUSE_STAFF".equals(role) || "MANAGER".equals(role)) {
            return transferRepository.findByFromWarehouseIdOrToWarehouseId(warehouseId, warehouseId, pageable)
                    .map(this::toResponse);
        }
        return transferRepository.findAll(pageable).map(this::toResponse);
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
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2)
    )
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

        if (fromStock.getCurrentStock() < suggestion.getQuantity()) {
            throw new InsufficientStockException(fromStock.getCurrentStock(), suggestion.getQuantity());
        }

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

    @Recover
    void recoverDispatch(ObjectOptimisticLockingFailureException ex,
                         String id, String staffId, String staffWarehouseId, String companyId) {
        throw new StockConflictException();
    }

    @Override
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2)
    )
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

    @Recover
    void recoverAccept(ObjectOptimisticLockingFailureException ex,
                       String id, String staffId, String staffWarehouseId, String companyId) {
        throw new StockConflictException();
    }

    @Override
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public void rejectDelivery(String id, String staffId, String staffWarehouseId, String companyId) {
        TransferSuggestion suggestion = findOrThrow(id);
        if (suggestion.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new InvalidStateTransitionException("TransferSuggestion", suggestion.getStatus().name(), "reject delivery");
        }
        if (!suggestion.getToWarehouseId().equals(staffWarehouseId)) {
            throw new WarehouseNotAssignedException(suggestion.getToWarehouseId());
        }

        StockLevel fromStock = stockLevelRepository
                .findByProductIdAndWarehouseId(suggestion.getProductId(), suggestion.getFromWarehouseId())
                .orElseThrow(() -> new StockLevelNotFoundException(suggestion.getProductId(), suggestion.getFromWarehouseId()));

        fromStock.setCurrentStock(fromStock.getCurrentStock() + suggestion.getQuantity());
        stockLevelRepository.save(fromStock);

        stockMovementRepository.save(StockMovement.builder()
                .productId(suggestion.getProductId())
                .warehouseId(suggestion.getFromWarehouseId())
                .type(MovementType.TRANSFER_IN)
                .quantity(suggestion.getQuantity())
                .referenceId(id)
                .createdBy(staffId)
                .build());

        suggestion.setStatus(TransferStatus.DELIVERY_REJECTED);
        transferRepository.save(suggestion);

        RestockAlert alert = restockAlertService.createAlert(
                suggestion.getProductId(), suggestion.getToWarehouseId(), 0, 0, companyId);
        String alertId = alert != null ? alert.getId() : null;
        eventProducer.publishTransferRejected(companyId, id, staffId, alertId);
    }

    @Recover
    void recoverRejectDelivery(ObjectOptimisticLockingFailureException ex,
                               String id, String staffId, String staffWarehouseId, String companyId) {
        throw new StockConflictException();
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
