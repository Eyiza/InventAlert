package com.inventalert.inventoryService.exception;

public class WarehouseNotFoundException extends RuntimeException {
    public WarehouseNotFoundException(String id) {
        super("Warehouse not found.");
    }
}
