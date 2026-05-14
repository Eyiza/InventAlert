package com.inventalert.inventoryService.exception;

public class StockNotChangedException extends RuntimeException {
    public StockNotChangedException(int currentStock, int threshold) {
        super("Current stock is still at or below the restock threshold. Confirm restocking is complete before resolving.");
    }
}
