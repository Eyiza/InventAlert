package com.inventalert.inventoryService.repository;

import com.inventalert.inventoryService.model.StockLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, String> {
    Optional<StockLevel> findByProductIdAndWarehouseId(String productId, String warehouseId);
    List<StockLevel> findByWarehouseIdOrderByCurrentStockAsc(String warehouseId);
    List<StockLevel> findByProductIdAndWarehouseIdNot(String productId, String excludedWarehouseId);
}
