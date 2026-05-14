package com.inventalert.identityService.exception;

public class WarehouseManagerConflictException extends IdentityServiceException {
    public WarehouseManagerConflictException(String warehouseId) {
        super("This warehouse already has a manager assigned.");
    }
}
