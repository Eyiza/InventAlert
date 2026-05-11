package com.inventalert.identityService.service;

import com.inventalert.identityService.dto.response.CompanyResponse;
import com.inventalert.identityService.model.Company;
import com.inventalert.identityService.model.CompanyStatus;
import com.inventalert.identityService.repository.CompanyRepository;
import com.inventalert.identityService.repository.UserRepository;
import com.inventalert.identityService.repository.WarehouseAssignmentRepository;
import com.inventalert.identityService.kafka.CompanyEventProducer;
import com.inventalert.identityService.service.impl.CompanyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;
    @Mock private WarehouseAssignmentRepository assignmentRepository;
    @Mock private CompanyEventProducer eventProducer;

    private CompanyServiceImpl companyService;

    @BeforeEach
    void setUp() {
        companyService = new CompanyServiceImpl(companyRepository, userRepository, assignmentRepository, eventProducer);
    }

    @Test
    void SuspendCompany_CheckIfSuccessfulTest() {
        String companyId = "company-1";

        Company activeCompany = Company.builder()
                .id(companyId)
                .companyName("Acme Ltd")
                .adminEmail("admin@acme.com")
                .status(CompanyStatus.ACTIVE)
                .build();

        Company suspendedCompany = Company.builder()
                .id(companyId)
                .companyName("Acme Ltd")
                .adminEmail("admin@acme.com")
                .status(CompanyStatus.SUSPENDED)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(activeCompany));
        when(companyRepository.save(activeCompany)).thenReturn(suspendedCompany);

        CompanyResponse response = companyService.suspendCompany(companyId);

        assertThat(response.status()).isEqualTo(CompanyStatus.SUSPENDED);
        assertThat(response.id()).isEqualTo(companyId);
        verify(companyRepository).save(activeCompany);
    }
}
