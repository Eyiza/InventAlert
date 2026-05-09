package com.inventalert.identityService.exception;

public class WarehouseManagerConflictException extends IdentityServiceException {
    public WarehouseManagerConflictException(String warehouseId) {
        super("Warehouse " + warehouseId + " already has an active manager assigned");
    }
}
