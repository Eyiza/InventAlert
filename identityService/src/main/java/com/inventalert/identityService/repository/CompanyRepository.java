package com.inventalert.identityService.repository;

import com.inventalert.identityService.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, String> {
    Optional<Company> findByAdminEmail(String adminEmail);
    boolean existsByAdminEmail(String adminEmail);
}
