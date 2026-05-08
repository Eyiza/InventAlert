package com.inventalert.identityService.security.exception;

import com.inventalert.identityService.exception.IdentityServiceException;

public class SuspendedCompanyException extends IdentityServiceException {
    public SuspendedCompanyException() {
        super("This company account is suspended");
    }
}
