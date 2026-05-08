package com.inventalert.identityService.exception;

public class UserNotFoundException extends IdentityServiceException {
    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
    }
}
