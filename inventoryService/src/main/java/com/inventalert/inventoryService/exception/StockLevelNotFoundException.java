package com.inventalert.inventoryService.exception;

public class StockLevelNotFoundException extends RuntimeException {
    public StockLevelNotFoundException(String productId, String warehouseId) {
        super("Stock level not found for product=" + productId + ", warehouse=" + warehouseId);
    }
}
