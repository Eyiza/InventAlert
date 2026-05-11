package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.response.StockLevelResponse;
import com.inventalert.inventoryService.model.StockLevel;

import java.util.List;

public interface StockLevelService {
    StockLevel getOrCreate(String productId, String warehouseId);
    void setWarehouseThreshold(String productId, String warehouseId, int threshold);
    List<StockLevelResponse> getAllStockLevels();
    List<StockLevelResponse> getStockForWarehouse(String warehouseId);
}
