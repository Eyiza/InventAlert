package com.inventalert.identityService.exception;

public class EmailAlreadyExistsException extends IdentityServiceException {
    public EmailAlreadyExistsException(String email) {
        super("Email already in use: " + email);
    }
}
