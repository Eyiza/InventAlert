package com.inventalert.inventoryService.service.impl;

import com.inventalert.inventoryService.model.MovementType;
import com.inventalert.inventoryService.model.StockLevel;
import com.inventalert.inventoryService.model.StockMovement;
import com.inventalert.inventoryService.repository.StockLevelRepository;
import com.inventalert.inventoryService.repository.StockMovementRepository;
import com.inventalert.inventoryService.service.VelocityCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VelocityCalculationServiceImpl implements VelocityCalculationService {

    private final StockMovementRepository movementRepository;
    private final StockLevelRepository stockLevelRepository;

    @Override
    @Async
    @Transactional
    public void recalculate(String productId, String warehouseId) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<StockMovement> recentSales = movementRepository
                .findByProductIdAndWarehouseIdAndTypeAndCreatedAtAfter(
                        productId, warehouseId, MovementType.OUTBOUND_SALE, sevenDaysAgo);

        int totalOutbound = recentSales.stream().mapToInt(StockMovement::getQuantity).sum();
        BigDecimal velocity = BigDecimal.valueOf(totalOutbound)
                .divide(BigDecimal.valueOf(7), 4, RoundingMode.HALF_UP);

        StockLevel level = stockLevelRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow();

        level.setVelocityPerDay(velocity);

        if (velocity.compareTo(BigDecimal.ZERO) > 0) {
            int days = BigDecimal.valueOf(level.getCurrentStock())
                    .divide(velocity, 0, RoundingMode.DOWN)
                    .intValue();
            level.setDaysUntilEmpty(days);
        } else {
            level.setDaysUntilEmpty(null);
        }

        stockLevelRepository.save(level);
    }
}
