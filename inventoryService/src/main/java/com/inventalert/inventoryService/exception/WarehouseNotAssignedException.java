package com.inventalert.inventoryService.exception;

public class WarehouseNotAssignedException extends RuntimeException {
    public WarehouseNotAssignedException(String warehouseId) {
        super("Staff is not assigned to warehouse: " + warehouseId);
    }
}
