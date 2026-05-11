package com.inventalert.inventoryService.controller;

import com.inventalert.inventoryService.dto.request.SubmitReconciliationRequest;
import com.inventalert.inventoryService.dto.response.ReconciliationResponse;
import com.inventalert.inventoryService.security.model.JwtUser;
import com.inventalert.inventoryService.service.ReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reconciliations")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public ResponseEntity<ReconciliationResponse> submit(
            @Valid @RequestBody SubmitReconciliationRequest request) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reconciliationService.submit(request, principal.getUserId(), principal.getCompanyId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ReconciliationResponse>> list() {
        return ResponseEntity.ok(reconciliationService.list());
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> approve(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        reconciliationService.approve(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> reject(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        reconciliationService.reject(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
