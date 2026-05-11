package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.model.MovementType;
import com.inventalert.inventoryService.model.StockLevel;
import com.inventalert.inventoryService.model.StockMovement;
import com.inventalert.inventoryService.repository.StockLevelRepository;
import com.inventalert.inventoryService.repository.StockMovementRepository;
import com.inventalert.inventoryService.service.impl.VelocityCalculationServiceImpl;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VelocityCalculationServiceTest {

    @Mock
    private StockMovementRepository movementRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @InjectMocks
    private VelocityCalculationServiceImpl velocityService;

    private StockLevel stockLevel;

    @BeforeEach
    void setUp() {
        stockLevel = StockLevel.builder()
                .id("sl1").productId("p1").warehouseId("w1")
                .currentStock(70).threshold(10).velocityPerDay(BigDecimal.ZERO).build();
    }

    @Test
    void recalculate_computesCorrectVelocityAndDaysUntilEmpty() {
        List<StockMovement> sales = List.of(
                StockMovement.builder().quantity(35).type(MovementType.OUTBOUND_SALE).build(),
                StockMovement.builder().quantity(35).type(MovementType.OUTBOUND_SALE).build()
        );

        when(movementRepository.findByProductIdAndWarehouseIdAndTypeAndCreatedAtAfter(
                eq("p1"), eq("w1"), eq(MovementType.OUTBOUND_SALE), any()))
                .thenReturn(sales);
        when(stockLevelRepository.findByProductIdAndWarehouseId("p1", "w1"))
                .thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(any())).thenReturn(stockLevel);

        velocityService.recalculate("p1", "w1");

        // 70 units / 7 days = 10.0 velocity
        assertThat(stockLevel.getVelocityPerDay()).isEqualByComparingTo(new BigDecimal("10.0000"));
        // 70 current stock / 10 velocity = 7 days
        assertThat(stockLevel.getDaysUntilEmpty()).isEqualTo(7);
        verify(stockLevelRepository).save(stockLevel);
    }

    @Test
    void recalculate_setsNullDaysWhenVelocityIsZero() {
        when(movementRepository.findByProductIdAndWarehouseIdAndTypeAndCreatedAtAfter(
                eq("p1"), eq("w1"), eq(MovementType.OUTBOUND_SALE), any()))
                .thenReturn(List.of());
        when(stockLevelRepository.findByProductIdAndWarehouseId("p1", "w1"))
                .thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(any())).thenReturn(stockLevel);

        velocityService.recalculate("p1", "w1");

        assertThat(stockLevel.getVelocityPerDay()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stockLevel.getDaysUntilEmpty()).isNull();
    }
}
