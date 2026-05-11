package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.response.StockLevelResponse;
import com.inventalert.inventoryService.model.StockLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StockLevelService {
    StockLevel getOrCreate(String productId, String warehouseId);
    void setWarehouseThreshold(String productId, String warehouseId, int threshold);
    Page<StockLevelResponse> getAllStockLevels(Pageable pageable);
    List<StockLevelResponse> getStockForWarehouse(String warehouseId);
}
