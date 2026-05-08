package com.inventalert.identityService.service;

import com.inventalert.identityService.dto.response.CompanyResponse;
import com.inventalert.identityService.exception.CompanyNotFoundException;
import com.inventalert.identityService.model.Company;
import com.inventalert.identityService.model.CompanyStatus;
import com.inventalert.identityService.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyEventProducer eventProducer;

    public CompanyService(CompanyRepository companyRepository, CompanyEventProducer eventProducer) {
        this.companyRepository = companyRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> listAllCompanies() {
        return companyRepository.findAll()
                .stream().map(CompanyResponse::from).toList();
    }

    public CompanyResponse suspendCompany(String companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
        company.setStatus(CompanyStatus.SUSPENDED);
        return CompanyResponse.from(companyRepository.save(company));
    }

    public CompanyResponse reactivateCompany(String companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
        company.setStatus(CompanyStatus.ACTIVE);
        return CompanyResponse.from(companyRepository.save(company));
    }

    public void initiateOffboarding(String companyId) {
        companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
        eventProducer.publishCompanyOffboarded(companyId);
    }
}
