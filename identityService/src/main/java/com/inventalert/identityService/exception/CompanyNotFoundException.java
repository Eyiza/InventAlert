package com.inventalert.identityService.exception;

public class CompanyNotFoundException extends IdentityServiceException {
    public CompanyNotFoundException(String companyId) {
        super("Company not found.");
    }
}
