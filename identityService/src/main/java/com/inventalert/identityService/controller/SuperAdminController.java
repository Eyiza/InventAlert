package com.inventalert.identityService.controller;

import com.inventalert.identityService.dto.response.CompanyResponse;
import com.inventalert.identityService.security.model.JwtUser;
import com.inventalert.identityService.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "SuperAdmin", description = "Company administration — SUPER_ADMIN role required (except offboarding)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class SuperAdminController {

    private final CompanyService companyService;

    @Operation(
        summary = "List all companies",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of all companies"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content)
        }
    )
    @GetMapping("/api/superadmin/companies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<CompanyResponse>> listAllCompanies() {
        return ResponseEntity.ok(companyService.listAllCompanies());
    }

    @Operation(
        summary = "Suspend a company",
        description = "Sets company status to SUSPENDED. All users of that company receive 403 until reactivated.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Company suspended",
                content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Company not found", content = @Content)
        }
    )
    @PatchMapping("/api/superadmin/companies/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CompanyResponse> suspendCompany(
            @Parameter(description = "UUID of the company") @PathVariable String id) {
        return ResponseEntity.ok(companyService.suspendCompany(id));
    }

    @Operation(
        summary = "Reactivate a suspended company",
        responses = {
            @ApiResponse(responseCode = "200", description = "Company reactivated",
                content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Company not found", content = @Content)
        }
    )
    @PatchMapping("/api/superadmin/companies/{id}/reactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CompanyResponse> reactivateCompany(
            @Parameter(description = "UUID of the company") @PathVariable String id) {
        return ResponseEntity.ok(companyService.reactivateCompany(id));
    }

    @Operation(
        summary = "Offboard own company",
        description = "Permanently removes the authenticated admin's company and publishes a company.offboarded Kafka event. Irreversible.",
        security = @SecurityRequirement(name = "bearerAuth"),
        tags = {"SuperAdmin"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Company offboarded successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content)
        }
    )
    @DeleteMapping("/api/companies/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> initiateOffboarding(
            @AuthenticationPrincipal JwtUser principal) {
        companyService.initiateOffboarding(principal.getCompanyId());
        return ResponseEntity.ok().build();
    }
}
