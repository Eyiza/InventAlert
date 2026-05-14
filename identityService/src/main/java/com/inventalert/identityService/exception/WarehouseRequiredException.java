package com.inventalert.identityService.exception;

import com.inventalert.identityService.model.Role;

public class WarehouseRequiredException extends IdentityServiceException {
    public WarehouseRequiredException(Role role) {
        super("A warehouse assignment is required for this role. Please select a warehouse before saving.");
    }
}
