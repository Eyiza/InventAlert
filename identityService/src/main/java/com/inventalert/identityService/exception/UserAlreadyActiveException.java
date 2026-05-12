package com.inventalert.identityService.exception;

public class UserAlreadyActiveException extends IdentityServiceException {
    public UserAlreadyActiveException(String userId) {
        super("User is already active: " + userId);
    }
}
