package com.inventalert.identityService.security.exception;

import com.inventalert.identityService.exception.IdentityServiceException;

public class InvalidCredentialsException extends IdentityServiceException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
