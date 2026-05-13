package com.inventalert.identityService.controller;

import com.inventalert.identityService.dto.request.UpdateCompanyRequest;
import com.inventalert.identityService.dto.response.CompanyResponse;
import com.inventalert.identityService.security.model.JwtUser;
import com.inventalert.identityService.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompanyResponse> getMyCompany(@AuthenticationPrincipal JwtUser principal) {
        return ResponseEntity.ok(companyService.getMyCompany(principal.getCompanyId()));
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyResponse> updateMyCompany(
            @AuthenticationPrincipal JwtUser principal,
            @RequestBody UpdateCompanyRequest request) {
        return ResponseEntity.ok(companyService.updateMyCompany(principal.getCompanyId(), request));
    }
}
