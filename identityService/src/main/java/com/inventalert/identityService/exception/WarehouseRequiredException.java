package com.inventalert.identityService.exception;

import com.inventalert.identityService.model.Role;

public class WarehouseRequiredException extends IdentityServiceException {
    public WarehouseRequiredException(Role role) {
        super("warehouseId is required for role: " + role);
    }
}
