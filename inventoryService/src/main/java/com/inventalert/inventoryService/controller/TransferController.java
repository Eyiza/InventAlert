package com.inventalert.inventoryService.controller;

import com.inventalert.inventoryService.dto.request.StaffInitiateTransferRequest;
import com.inventalert.inventoryService.dto.response.TransferSuggestionResponse;
import com.inventalert.inventoryService.security.model.JwtUser;
import com.inventalert.inventoryService.service.TransferService;
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

@Tag(name = "Transfers", description = "Inter-warehouse transfer lifecycle: SUGGESTED → APPROVED → IN_TRANSIT → COMPLETED (or REJECTED / DELIVERY_REJECTED)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "Initiate a transfer (staff)",
               description = "Warehouse staff manually initiates a transfer suggestion to another warehouse. The system auto-suggests transfers when threshold checks detect a surplus elsewhere.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Transfer suggestion created"),
        @ApiResponse(responseCode = "403", description = "Staff not assigned to source warehouse")
    })
    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public ResponseEntity<TransferSuggestionResponse> initiateTransfer(
            @Valid @RequestBody StaffInitiateTransferRequest request) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transferService.initiateByStaff(request, principal.getUserId(), principal.getCompanyId()));
    }

    @Operation(summary = "List transfers",
               description = "Returns paginated transfer list. MANAGER sees all transfers; WAREHOUSE_STAFF sees only transfers involving their assigned warehouse.")
    @ApiResponse(responseCode = "200", description = "Paginated transfer list")
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','WAREHOUSE_STAFF')")
    public ResponseEntity<Page<TransferSuggestionResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(transferService.list(principal.getRole(), principal.getWarehouseId(), pageable));
    }

    @Operation(summary = "Approve a transfer", description = "Manager approves a SUGGESTED transfer, moving it to APPROVED. If rejected instead, a restock alert is escalated automatically.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Transfer approved"),
        @ApiResponse(responseCode = "409", description = "Transfer not in SUGGESTED state")
    })
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> approve(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        transferService.approve(id, principal.getUserId(), principal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reject a transfer suggestion", description = "Manager rejects the suggestion; the system automatically escalates to a restock alert.")
    @ApiResponse(responseCode = "204", description = "Transfer rejected, restock alert created")
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> reject(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        transferService.reject(id, principal.getUserId(), principal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Dispatch a transfer", description = "Staff at the source warehouse marks stock as dispatched. Stock is deducted from the source warehouse and transfer moves to IN_TRANSIT.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Transfer dispatched"),
        @ApiResponse(responseCode = "403", description = "Staff not assigned to source warehouse"),
        @ApiResponse(responseCode = "409", description = "Transfer not in APPROVED state")
    })
    @PatchMapping("/{id}/dispatch")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public ResponseEntity<Void> dispatch(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        transferService.dispatch(id, principal.getUserId(), principal.getWarehouseId(), principal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Accept delivery", description = "Staff at the destination warehouse confirms receipt. Stock is added to destination and transfer moves to COMPLETED.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Transfer completed"),
        @ApiResponse(responseCode = "403", description = "Staff not assigned to destination warehouse")
    })
    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public ResponseEntity<Void> accept(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        transferService.accept(id, principal.getUserId(), principal.getWarehouseId(), principal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reject delivery", description = "Staff rejects the received goods. Stock is restored to the source warehouse and a restock alert is raised for the destination.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Delivery rejected, stock restored"),
        @ApiResponse(responseCode = "403", description = "Staff not assigned to destination warehouse"),
        @ApiResponse(responseCode = "409", description = "Transfer not in IN_TRANSIT state")
    })
    @PatchMapping("/{id}/reject-delivery")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public ResponseEntity<Void> rejectDelivery(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        transferService.rejectDelivery(id, principal.getUserId(), principal.getWarehouseId(), principal.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
