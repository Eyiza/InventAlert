package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.response.TransferSuggestionResponse;
import com.inventalert.inventoryService.exception.InvalidStateTransitionException;
import com.inventalert.inventoryService.exception.TransferNotFoundException;
import com.inventalert.inventoryService.exception.WarehouseNotAssignedException;
import com.inventalert.inventoryService.kafka.TransferEventProducer;
import com.inventalert.inventoryService.model.*;
import com.inventalert.inventoryService.repository.*;
import com.inventalert.inventoryService.service.impl.TransferServiceImpl;
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
class TransferServiceTest {

    @Mock
    private TransferSuggestionRepository transferRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private RestockAlertService restockAlertService;

    @Mock
    private GoogleMapsService googleMapsService;

    @Mock
    private TransferEventProducer eventProducer;

    @InjectMocks
    private TransferServiceImpl transferService;

    private TransferSuggestion suggested;
    private TransferSuggestion approved;
    private TransferSuggestion inTransit;
    private Warehouse fromWarehouse;
    private Warehouse toWarehouse;
    private StockLevel fromStock;

    @BeforeEach
    void setUp() {
        fromWarehouse = Warehouse.builder().id("w1").name("From").address("A")
                .latitude(new BigDecimal("40.0")).longitude(new BigDecimal("-74.0")).isActive(true).build();
        toWarehouse = Warehouse.builder().id("w2").name("To").address("B")
                .latitude(new BigDecimal("41.0")).longitude(new BigDecimal("-73.0")).isActive(true).build();

        fromStock = StockLevel.builder().id("sl1").productId("p1").warehouseId("w1")
                .currentStock(100).threshold(10).velocityPerDay(BigDecimal.ZERO).build();

        suggested = TransferSuggestion.builder().id("t1").productId("p1")
                .fromWarehouseId("w1").toWarehouseId("w2").quantity(20)
                .status(TransferStatus.SUGGESTED).build();

        approved = TransferSuggestion.builder().id("t2").productId("p1")
                .fromWarehouseId("w1").toWarehouseId("w2").quantity(20)
                .status(TransferStatus.APPROVED).build();

        inTransit = TransferSuggestion.builder().id("t3").productId("p1")
                .fromWarehouseId("w1").toWarehouseId("w2").quantity(20)
                .status(TransferStatus.IN_TRANSIT).build();
    }

    @Test
    void approve_changesStatusToApprovedAndPublishes() {
        when(transferRepository.findById("t1")).thenReturn(Optional.of(suggested));
        when(transferRepository.save(any())).thenReturn(suggested);

        transferService.approve("t1", "manager1", "company1");

        assertThat(suggested.getStatus()).isEqualTo(TransferStatus.APPROVED);
        verify(eventProducer).publishTransferApproved(any(), any(), any());
    }

    @Test
    void approve_throwsWhenNotSuggested() {
        suggested.setStatus(TransferStatus.APPROVED);
        when(transferRepository.findById("t1")).thenReturn(Optional.of(suggested));

        assertThatThrownBy(() -> transferService.approve("t1", "manager1", "company1"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void reject_escalatesToAlertAndPublishes() {
        when(transferRepository.findById("t1")).thenReturn(Optional.of(suggested));
        when(transferRepository.save(any())).thenReturn(suggested);
        when(restockAlertService.createAlert(any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(RestockAlert.builder().id("alert1").build());

        transferService.reject("t1", "manager1", "company1");

        assertThat(suggested.getStatus()).isEqualTo(TransferStatus.REJECTED);
        verify(restockAlertService).createAlert(any(), any(), anyInt(), anyInt(), any());
        verify(eventProducer).publishTransferRejected(any(), any(), any(), any());
    }

    @Test
    void dispatch_validatesStaffWarehouseAndCreatesMovement() {
        when(transferRepository.findById("t2")).thenReturn(Optional.of(approved));
        when(stockLevelRepository.findByProductIdAndWarehouseId("p1", "w1"))
                .thenReturn(Optional.of(fromStock));
        when(transferRepository.save(any())).thenReturn(approved);

        transferService.dispatch("t2", "staff1", "w1", "company1");

        assertThat(approved.getStatus()).isEqualTo(TransferStatus.IN_TRANSIT);
        assertThat(fromStock.getCurrentStock()).isEqualTo(80); // 100 - 20
        verify(stockMovementRepository).save(any());
    }

    @Test
    void dispatch_throwsWhenStaffNotAssignedToFromWarehouse() {
        when(transferRepository.findById("t2")).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> transferService.dispatch("t2", "staff1", "w99", "company1"))
                .isInstanceOf(WarehouseNotAssignedException.class);
    }

    @Test
    void accept_completesTransferAndIncrementsDestinationStock() {
        StockLevel toStock = StockLevel.builder().id("sl2").productId("p1").warehouseId("w2")
                .currentStock(50).threshold(10).velocityPerDay(BigDecimal.ZERO).build();

        when(transferRepository.findById("t3")).thenReturn(Optional.of(inTransit));
        when(stockLevelRepository.findByProductIdAndWarehouseId("p1", "w2"))
                .thenReturn(Optional.of(toStock));
        when(transferRepository.save(any())).thenReturn(inTransit);

        transferService.accept("t3", "staff2", "w2", "company1");

        assertThat(inTransit.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(toStock.getCurrentStock()).isEqualTo(70); // 50 + 20
        verify(stockMovementRepository).save(any());
        verify(eventProducer).publishTransferAccepted(any(), any(), any());
    }

    @Test
    void accept_throwsWhenStaffNotAssignedToToWarehouse() {
        when(transferRepository.findById("t3")).thenReturn(Optional.of(inTransit));

        assertThatThrownBy(() -> transferService.accept("t3", "staff2", "w99", "company1"))
                .isInstanceOf(WarehouseNotAssignedException.class);
    }
}
