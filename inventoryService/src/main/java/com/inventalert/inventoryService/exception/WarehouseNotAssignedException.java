package com.inventalert.inventoryService.exception;

public class WarehouseNotAssignedException extends RuntimeException {
    public WarehouseNotAssignedException(String warehouseId) {
        super("You are not assigned to this warehouse.");
    }
}
