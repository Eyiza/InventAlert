package com.inventalert.identityService.exception;

public class InvalidResetTokenException extends IdentityServiceException {
    public InvalidResetTokenException() {
        super("Password reset token is invalid, expired, or already used");
    }
}
