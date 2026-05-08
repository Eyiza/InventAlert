package com.inventalert.identityService.security.exception;

public class SuspendedCompanyException extends RuntimeException {
    public SuspendedCompanyException() {
        super("This company account is suspended");
    }
}
