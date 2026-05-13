package com.inventalert.inventoryService.exception;

public class StockLevelNotFoundException extends RuntimeException {
    public StockLevelNotFoundException(String productId, String warehouseId) {
        super("No stock record found for this product at the selected warehouse.");
    }
}
