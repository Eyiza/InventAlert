package com.inventalert.inventoryService.service.impl;

import com.inventalert.inventoryService.dto.response.StockLevelResponse;
import com.inventalert.inventoryService.exception.ProductNotFoundException;
import com.inventalert.inventoryService.exception.WarehouseNotFoundException;
import com.inventalert.inventoryService.model.Product;
import com.inventalert.inventoryService.model.StockLevel;
import com.inventalert.inventoryService.repository.ProductRepository;
import com.inventalert.inventoryService.repository.StockLevelRepository;
import com.inventalert.inventoryService.repository.WarehouseRepository;
import com.inventalert.inventoryService.service.StockLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockLevelServiceImpl implements StockLevelService {

    private final StockLevelRepository stockLevelRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public StockLevel getOrCreate(String productId, String warehouseId) {
        return stockLevelRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseGet(() -> {
                    Product product = productRepository.findByIdAndIsActiveTrue(productId)
                            .orElseThrow(() -> new ProductNotFoundException(productId));
                    warehouseRepository.findByIdAndIsActiveTrue(warehouseId)
                            .orElseThrow(() -> new WarehouseNotFoundException(warehouseId));

                    StockLevel level = StockLevel.builder()
                            .productId(productId)
                            .warehouseId(warehouseId)
                            .currentStock(0)
                            .threshold(product.getDefaultThreshold())
                            .velocityPerDay(BigDecimal.ZERO)
                            .build();
                    try {
                        return stockLevelRepository.save(level);
                    } catch (DataIntegrityViolationException e) {
                        return stockLevelRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                                .orElseThrow();
                    }
                });
    }

    @Override
    @Transactional
    public void setWarehouseThreshold(String productId, String warehouseId, int threshold) {
        StockLevel level = stockLevelRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseGet(() -> getOrCreate(productId, warehouseId));
        level.setThreshold(threshold);
        stockLevelRepository.save(level);
    }

    @Override
    public List<StockLevelResponse> getAllStockLevels() {
        return stockLevelRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<StockLevelResponse> getStockForWarehouse(String warehouseId) {
        return stockLevelRepository.findByWarehouseId(warehouseId).stream()
                .map(this::toResponse)
                .toList();
    }

    private StockLevelResponse toResponse(StockLevel s) {
        return StockLevelResponse.builder()
                .id(s.getId())
                .productId(s.getProductId())
                .warehouseId(s.getWarehouseId())
                .currentStock(s.getCurrentStock())
                .threshold(s.getThreshold())
                .velocityPerDay(s.getVelocityPerDay())
                .daysUntilEmpty(s.getDaysUntilEmpty())
                .build();
    }
}
