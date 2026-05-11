package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.request.SubmitReconciliationRequest;
import com.inventalert.inventoryService.dto.response.ReconciliationResponse;
import com.inventalert.inventoryService.exception.InvalidStateTransitionException;
import com.inventalert.inventoryService.exception.ReconciliationNotFoundException;
import com.inventalert.inventoryService.exception.SelfApprovalException;
import com.inventalert.inventoryService.model.*;
import com.inventalert.inventoryService.repository.ReconciliationRepository;
import com.inventalert.inventoryService.repository.StockLevelRepository;
import com.inventalert.inventoryService.repository.StockMovementRepository;
import com.inventalert.inventoryService.service.impl.ReconciliationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private ReconciliationRepository reconciliationRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private com.inventalert.inventoryService.kafka.ReconciliationEventProducer eventProducer;

    @InjectMocks
    private ReconciliationServiceImpl reconciliationService;

    private StockLevel stockLevel;
    private Reconciliation pendingRecon;

    @BeforeEach
    void setUp() {
        stockLevel = StockLevel.builder()
                .id("sl1").productId("p1").warehouseId("w1")
                .currentStock(100).threshold(10).velocityPerDay(BigDecimal.ZERO).build();

        pendingRecon = Reconciliation.builder()
                .id("r1").productId("p1").warehouseId("w1")
                .systemCount(100).physicalCount(90).discrepancy(-10)
                .reason("Discrepancy found").status(ReconciliationStatus.PENDING_APPROVAL)
                .createdBy("staff1").build();
    }

    @Test
    void submit_createsReconciliationWithCorrectDiscrepancy() {
        SubmitReconciliationRequest req = new SubmitReconciliationRequest();
        req.setProductId("p1");
        req.setWarehouseId("w1");
        req.setPhysicalCount(90);
        req.setReason("Discrepancy found");

        when(stockLevelRepository.findByProductIdAndWarehouseId("p1", "w1"))
                .thenReturn(Optional.of(stockLevel));
        when(reconciliationRepository.save(any())).thenReturn(pendingRecon);

        ReconciliationResponse response = reconciliationService.submit(req, "staff1", "company1");

        assertThat(response.getDiscrepancy()).isEqualTo(-10);
        assertThat(response.getStatus()).isEqualTo("PENDING_APPROVAL");
        verify(reconciliationRepository).save(any());
        verify(eventProducer).publishReconciliationRequested(any(), any(), any(), any());
    }

    @Test
    void approve_throwsSelfApprovalExceptionWhenSameUser() {
        when(reconciliationRepository.findById("r1")).thenReturn(Optional.of(pendingRecon));

        assertThatThrownBy(() -> reconciliationService.approve("r1", "staff1"))
                .isInstanceOf(SelfApprovalException.class);
    }

    @Test
    void approve_adjustsStockAndSavesMovement() {
        when(reconciliationRepository.findById("r1")).thenReturn(Optional.of(pendingRecon));
        when(stockLevelRepository.findByProductIdAndWarehouseId("p1", "w1"))
                .thenReturn(Optional.of(stockLevel));
        when(reconciliationRepository.save(any())).thenReturn(pendingRecon);
        when(stockMovementRepository.save(any())).thenReturn(null);

        reconciliationService.approve("r1", "manager1");

        assertThat(pendingRecon.getStatus()).isEqualTo(ReconciliationStatus.APPROVED);
        assertThat(stockLevel.getCurrentStock()).isEqualTo(90); // 100 + (-10)
        verify(stockMovementRepository).save(any());
    }

    @Test
    void approve_throwsWhenNotPendingApproval() {
        pendingRecon.setStatus(ReconciliationStatus.APPROVED);
        when(reconciliationRepository.findById("r1")).thenReturn(Optional.of(pendingRecon));

        assertThatThrownBy(() -> reconciliationService.approve("r1", "manager1"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void reject_leavesStockUnchanged() {
        when(reconciliationRepository.findById("r1")).thenReturn(Optional.of(pendingRecon));
        when(reconciliationRepository.save(any())).thenReturn(pendingRecon);

        reconciliationService.reject("r1", "manager1");

        assertThat(pendingRecon.getStatus()).isEqualTo(ReconciliationStatus.REJECTED);
        assertThat(stockLevel.getCurrentStock()).isEqualTo(100); // unchanged
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void list_returnsAllReconciliations() {
        when(reconciliationRepository.findAll()).thenReturn(List.of(pendingRecon));

        List<ReconciliationResponse> result = reconciliationService.list();

        assertThat(result).hasSize(1);
    }
}
