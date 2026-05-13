package com.inventalert.identityService.security.exception;

import com.inventalert.identityService.exception.IdentityServiceException;

public class DeactivatedUserException extends IdentityServiceException {
    public DeactivatedUserException() {
        super("Your account has been deactivated. Please contact your company administrator.");
    }
}
