package com.inventalert.identityService.controller;

import com.inventalert.identityService.dto.request.ResolveComplaintRequest;
import com.inventalert.identityService.dto.request.SubmitComplaintRequest;
import com.inventalert.identityService.dto.response.ComplaintResponse;
import com.inventalert.identityService.security.model.JwtUser;
import com.inventalert.identityService.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComplaintResponse> submit(
            @AuthenticationPrincipal JwtUser principal,
            @Valid @RequestBody SubmitComplaintRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(complaintService.submit(principal.getCompanyId(), principal.getUserId(), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<ComplaintResponse>> list(@AuthenticationPrincipal JwtUser principal) {
        if ("SUPER_ADMIN".equals(principal.getRole())) {
            return ResponseEntity.ok(complaintService.listAll());
        }
        return ResponseEntity.ok(complaintService.listForCompany(principal.getCompanyId()));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ComplaintResponse> resolve(
            @PathVariable String id,
            @Valid @RequestBody ResolveComplaintRequest request) {
        return ResponseEntity.ok(complaintService.resolve(id, request));
    }
}
