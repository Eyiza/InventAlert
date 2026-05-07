package com.inventalert.identityService.dto.response;

import com.inventalert.identityService.model.Company;
import com.inventalert.identityService.model.CompanyStatus;

import java.time.LocalDateTime;

public record CompanyResponse(
        String id,
        String companyName,
        String adminEmail,
        CompanyStatus status,
        LocalDateTime createdAt
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getCompanyName(),
                company.getAdminEmail(),
                company.getStatus(),
                company.getCreatedAt()
        );
    }
}
