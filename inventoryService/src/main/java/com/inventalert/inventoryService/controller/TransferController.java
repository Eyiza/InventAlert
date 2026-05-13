package com.inventalert.inventoryService.controller;

import com.inventalert.inventoryService.dto.request.StaffInitiateTransferRequest;
import com.inventalert.inventoryService.dto.response.TransferSuggestionResponse;
import com.inventalert.inventoryService.security.model.JwtUser;
import com.inventalert.inventoryService.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public ResponseEntity<TransferSuggestionResponse> initiateTransfer(
            @Valid @RequestBody StaffInitiateTransferRequest request) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transferService.initiateByStaff(request, principal.getUserId(), principal.getCompanyId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','WAREHOUSE_STAFF')")
    public ResponseEntity<Page<TransferSuggestionResponse>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(transferService.list(principal.getRole(), principal.getWarehouseId(), pageable));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> approve(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        transferService.approve(id, principal.getUserId(), principal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> reject(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        transferService.reject(id, principal.getUserId(), principal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/dispatch")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public ResponseEntity<Void> dispatch(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        transferService.dispatch(id, principal.getUserId(), principal.getWarehouseId(), principal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public ResponseEntity<Void> accept(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        transferService.accept(id, principal.getUserId(), principal.getWarehouseId(), principal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reject-delivery")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public ResponseEntity<Void> rejectDelivery(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        transferService.rejectDelivery(id, principal.getUserId(), principal.getWarehouseId(), principal.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
