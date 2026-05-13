package com.inventalert.identityService.exception;

public class ComplaintNotFoundException extends IdentityServiceException {
    public ComplaintNotFoundException(String id) {
        super("Support ticket not found.");
    }
}
