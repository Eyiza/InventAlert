package com.inventalert.identityService.exception;

public class UserAlreadyDeactivatedException extends RuntimeException {
    public UserAlreadyDeactivatedException(String userId) {
        super("User is already deactivated: " + userId);
    }
}
