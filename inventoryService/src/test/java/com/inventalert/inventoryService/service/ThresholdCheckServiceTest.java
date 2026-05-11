package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.model.AlertStatus;
import com.inventalert.inventoryService.model.StockLevel;
import com.inventalert.inventoryService.repository.RestockAlertRepository;
import com.inventalert.inventoryService.repository.StockLevelRepository;
import com.inventalert.inventoryService.service.impl.ThresholdCheckServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThresholdCheckServiceTest {

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private RestockAlertRepository restockAlertRepository;

    @Mock
    private RestockAlertService restockAlertService;

    @Mock
    private TransferService transferService;

    @InjectMocks
    private ThresholdCheckServiceImpl thresholdCheckService;

    private StockLevel lowStock;
    private StockLevel surplusStock;

    @BeforeEach
    void setUp() {
        lowStock = StockLevel.builder()
                .id("sl1").productId("p1").warehouseId("w1")
                .currentStock(5).threshold(20).velocityPerDay(BigDecimal.ZERO).build();

        surplusStock = StockLevel.builder()
                .id("sl2").productId("p1").warehouseId("w2")
                .currentStock(100).threshold(10).velocityPerDay(BigDecimal.ZERO).build();
    }

    @Test
    void checkThreshold_doesNothingWhenStockAboveThreshold() {
        lowStock.setCurrentStock(30); // above threshold of 20
        when(stockLevelRepository.findByProductIdAndWarehouseId("p1", "w1"))
                .thenReturn(Optional.of(lowStock));

        thresholdCheckService.checkThreshold("p1", "w1", "company1");

        verifyNoInteractions(restockAlertService, transferService);
    }

    @Test
    void checkThreshold_createsAlertWhenNoCandidates() {
        when(stockLevelRepository.findByProductIdAndWarehouseId("p1", "w1"))
                .thenReturn(Optional.of(lowStock));
        when(restockAlertRepository.existsByProductIdAndWarehouseIdAndStatus("p1", "w1", AlertStatus.OPEN))
                .thenReturn(false);
        when(stockLevelRepository.findByProductIdAndWarehouseIdNot("p1", "w1"))
                .thenReturn(List.of()); // no surplus candidates

        thresholdCheckService.checkThreshold("p1", "w1", "company1");

        verify(restockAlertService).createAlert(eq("p1"), eq("w1"), anyInt(), anyInt(), eq("company1"));
        verify(transferService, never()).createSuggestion(any(), any(), any(), anyInt(), any());
    }

    @Test
    void checkThreshold_createsSuggestionWhenSurplusExists() {
        when(stockLevelRepository.findByProductIdAndWarehouseId("p1", "w1"))
                .thenReturn(Optional.of(lowStock));
        when(restockAlertRepository.existsByProductIdAndWarehouseIdAndStatus("p1", "w1", AlertStatus.OPEN))
                .thenReturn(false);
        // surplusStock has 100 - 10 = 90 surplus, shortage = 20 - 5 = 15 -> qualifies
        when(stockLevelRepository.findByProductIdAndWarehouseIdNot("p1", "w1"))
                .thenReturn(List.of(surplusStock));

        thresholdCheckService.checkThreshold("p1", "w1", "company1");

        verify(transferService).createSuggestion(eq("p1"), eq("w1"), anyList(), eq(15), eq("company1"));
        verify(restockAlertService, never()).createAlert(any(), any(), anyInt(), anyInt(), any());
    }

    @Test
    void checkThreshold_skipsWhenOpenAlertAlreadyExists() {
        when(stockLevelRepository.findByProductIdAndWarehouseId("p1", "w1"))
                .thenReturn(Optional.of(lowStock));
        when(restockAlertRepository.existsByProductIdAndWarehouseIdAndStatus("p1", "w1", AlertStatus.OPEN))
                .thenReturn(true);

        thresholdCheckService.checkThreshold("p1", "w1", "company1");

        verifyNoInteractions(restockAlertService, transferService);
    }
}
