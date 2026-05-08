package com.inventalert.identityService.controller;

import com.inventalert.identityService.dto.response.CompanyResponse;
import com.inventalert.identityService.security.model.JwtUser;
import com.inventalert.identityService.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SuperAdminController {

    private final CompanyService companyService;

    @GetMapping("/api/superadmin/companies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<CompanyResponse>> listAllCompanies() {
        return ResponseEntity.ok(companyService.listAllCompanies());
    }

    @PatchMapping("/api/superadmin/companies/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CompanyResponse> suspendCompany(@PathVariable String id) {
        return ResponseEntity.ok(companyService.suspendCompany(id));
    }

    @PatchMapping("/api/superadmin/companies/{id}/reactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CompanyResponse> reactivateCompany(@PathVariable String id) {
        return ResponseEntity.ok(companyService.reactivateCompany(id));
    }

    @DeleteMapping("/api/companies/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> initiateOffboarding(
            @AuthenticationPrincipal JwtUser principal) {
        companyService.initiateOffboarding(principal.getCompanyId());
        return ResponseEntity.ok().build();
    }
}
