package com.inventalert.identityService.service;

import com.inventalert.identityService.dto.request.UpdateCompanyRequest;
import com.inventalert.identityService.dto.response.CompanyResponse;

import java.util.List;

public interface CompanyService {
    List<CompanyResponse> listAllCompanies();
    CompanyResponse suspendCompany(String companyId);
    CompanyResponse reactivateCompany(String companyId);
    void initiateOffboarding(String companyId);
    CompanyResponse getMyCompany(String companyId);
    CompanyResponse updateMyCompany(String companyId, UpdateCompanyRequest request);
}
