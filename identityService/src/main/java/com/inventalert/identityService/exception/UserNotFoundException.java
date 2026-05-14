package com.inventalert.identityService.exception;

public class UserNotFoundException extends IdentityServiceException {
    public UserNotFoundException(String userId) {
        super("User account not found: " + userId);
    }
}
