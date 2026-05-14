package com.inventalert.inventoryService.controller;

import com.inventalert.inventoryService.dto.request.SubmitReconciliationRequest;
import com.inventalert.inventoryService.dto.response.ReconciliationResponse;
import com.inventalert.inventoryService.security.model.JwtUser;
import com.inventalert.inventoryService.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reconciliations", description = "Physical stock count submission (staff) and manager approval workflow")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/reconciliations")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @Operation(summary = "Submit a reconciliation",
               description = "Warehouse staff submits the physical count for a product. The system calculates the discrepancy against the system count and creates a PENDING_APPROVAL record. Self-approval is blocked — the approver must be a different user.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reconciliation submitted"),
        @ApiResponse(responseCode = "404", description = "Stock level not found for product/warehouse combination")
    })
    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public ResponseEntity<ReconciliationResponse> submit(
            @Valid @RequestBody SubmitReconciliationRequest request) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reconciliationService.submit(request, principal.getUserId(), principal.getCompanyId()));
    }

    @Operation(summary = "List reconciliations",
               description = "Returns paginated reconciliations. ADMIN sees all; MANAGER sees only reconciliations for their assigned warehouse.")
    @ApiResponse(responseCode = "200", description = "Paginated reconciliation list")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Page<ReconciliationResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if ("ADMIN".equals(principal.getRole())) {
            return ResponseEntity.ok(reconciliationService.list(pageable));
        }
        return ResponseEntity.ok(reconciliationService.listByWarehouse(principal.getWarehouseId(), pageable));
    }

    @Operation(summary = "Approve a reconciliation",
               description = "Manager approves the physical count. Stock is adjusted to match the physical count and a corrective stock movement is recorded.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Approved — stock adjusted"),
        @ApiResponse(responseCode = "403", description = "Self-approval not permitted"),
        @ApiResponse(responseCode = "409", description = "Reconciliation not in PENDING_APPROVAL state")
    })
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> approve(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        reconciliationService.approve(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reject a reconciliation",
               description = "Manager rejects the submission. No stock adjustment is made.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Rejected — stock unchanged"),
        @ApiResponse(responseCode = "409", description = "Reconciliation not in PENDING_APPROVAL state")
    })
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> reject(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        reconciliationService.reject(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
