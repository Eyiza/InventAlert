package com.inventalert.inventoryService.service.impl;

import com.inventalert.inventoryService.dto.request.SubmitReconciliationRequest;
import com.inventalert.inventoryService.dto.response.ReconciliationResponse;
import com.inventalert.inventoryService.exception.InsufficientStockException;
import com.inventalert.inventoryService.exception.InvalidStateTransitionException;
import com.inventalert.inventoryService.exception.ReconciliationNotFoundException;
import com.inventalert.inventoryService.exception.SelfApprovalException;
import com.inventalert.inventoryService.exception.StockConflictException;
import com.inventalert.inventoryService.exception.StockLevelNotFoundException;
import com.inventalert.inventoryService.kafka.ReconciliationEventProducer;
import com.inventalert.inventoryService.model.*;
import com.inventalert.inventoryService.repository.ReconciliationRepository;
import com.inventalert.inventoryService.repository.StockLevelRepository;
import com.inventalert.inventoryService.repository.StockMovementRepository;
import com.inventalert.inventoryService.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private final ReconciliationRepository reconciliationRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ReconciliationEventProducer eventProducer;

    @Override
    @Transactional
    public ReconciliationResponse submit(SubmitReconciliationRequest request, String staffId, String companyId) {
        StockLevel level = stockLevelRepository
                .findByProductIdAndWarehouseId(request.getProductId(), request.getWarehouseId())
                .orElseThrow(() -> new StockLevelNotFoundException(request.getProductId(), request.getWarehouseId()));

        int systemCount = level.getCurrentStock();
        int discrepancy = request.getPhysicalCount() - systemCount;

        Reconciliation recon = Reconciliation.builder()
                .productId(request.getProductId())
                .warehouseId(request.getWarehouseId())
                .systemCount(systemCount)
                .physicalCount(request.getPhysicalCount())
                .discrepancy(discrepancy)
                .reason(request.getReason())
                .status(ReconciliationStatus.PENDING_APPROVAL)
                .createdBy(staffId)
                .build();

        Reconciliation saved = reconciliationRepository.save(recon);
        eventProducer.publishReconciliationRequested(companyId, saved.getId(), request.getWarehouseId(), staffId);
        return toResponse(saved);
    }

    @Override
    public List<ReconciliationResponse> list() {
        return reconciliationRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public void approve(String id, String managerId) {
        Reconciliation recon = findOrThrow(id);

        if (recon.getStatus() != ReconciliationStatus.PENDING_APPROVAL) {
            throw new InvalidStateTransitionException("Reconciliation", recon.getStatus().name(), "approve");
        }
        if (recon.getCreatedBy().equals(managerId)) {
            throw new SelfApprovalException();
        }

        StockLevel level = stockLevelRepository
                .findByProductIdAndWarehouseId(recon.getProductId(), recon.getWarehouseId())
                .orElseThrow(() -> new StockLevelNotFoundException(recon.getProductId(), recon.getWarehouseId()));

        int newStock = level.getCurrentStock() + recon.getDiscrepancy();
        if (newStock < 0) {
            throw new InsufficientStockException(level.getCurrentStock(), Math.abs(recon.getDiscrepancy()));
        }
        level.setCurrentStock(newStock);
        stockLevelRepository.save(level);

        StockMovement movement = StockMovement.builder()
                .productId(recon.getProductId())
                .warehouseId(recon.getWarehouseId())
                .type(MovementType.RECONCILIATION)
                .quantity(Math.abs(recon.getDiscrepancy()))
                .createdBy(managerId)
                .build();
        stockMovementRepository.save(movement);

        recon.setStatus(ReconciliationStatus.APPROVED);
        recon.setApprovedBy(managerId);
        reconciliationRepository.save(recon);
    }

    @Recover
    void recoverApprove(ObjectOptimisticLockingFailureException ex, String id, String managerId) {
        throw new StockConflictException();
    }

    @Override
    @Transactional
    public void reject(String id, String managerId) {
        Reconciliation recon = findOrThrow(id);

        if (recon.getStatus() != ReconciliationStatus.PENDING_APPROVAL) {
            throw new InvalidStateTransitionException("Reconciliation", recon.getStatus().name(), "reject");
        }
        recon.setStatus(ReconciliationStatus.REJECTED);
        reconciliationRepository.save(recon);
    }

    private Reconciliation findOrThrow(String id) {
        return reconciliationRepository.findById(id)
                .orElseThrow(() -> new ReconciliationNotFoundException(id));
    }

    private ReconciliationResponse toResponse(Reconciliation r) {
        return ReconciliationResponse.builder()
                .id(r.getId()).productId(r.getProductId()).warehouseId(r.getWarehouseId())
                .systemCount(r.getSystemCount()).physicalCount(r.getPhysicalCount())
                .discrepancy(r.getDiscrepancy()).reason(r.getReason())
                .status(r.getStatus().name()).createdBy(r.getCreatedBy())
                .approvedBy(r.getApprovedBy()).createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt())
                .build();
    }
}
