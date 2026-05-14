package com.inventalert.identityService.exception;

public class UserAlreadyActiveException extends IdentityServiceException {
    public UserAlreadyActiveException(String userId) {
        super("This account is already active.");
    }
}
