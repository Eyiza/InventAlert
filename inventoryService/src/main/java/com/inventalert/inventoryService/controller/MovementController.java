package com.inventalert.inventoryService.controller;

import com.inventalert.inventoryService.dto.request.RecordMovementRequest;
import com.inventalert.inventoryService.dto.response.StockMovementResponse;
import com.inventalert.inventoryService.model.MovementType;
import com.inventalert.inventoryService.security.model.JwtUser;
import com.inventalert.inventoryService.service.MovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Stock Movements", description = "Record stock intake and outbound sales; CSV batch import for bulk operations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/movements")
@RequiredArgsConstructor
public class MovementController {

    private final MovementService movementService;

    @Operation(summary = "Record a stock movement",
               description = "Records an INTAKE or OUTBOUND_SALE movement. After an outbound sale the system recalculates velocity and checks the restock threshold. Staff must be assigned to the target warehouse.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Movement recorded"),
        @ApiResponse(responseCode = "422", description = "Insufficient stock for outbound sale"),
        @ApiResponse(responseCode = "403", description = "Staff not assigned to this warehouse")
    })
    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE_STAFF') or (hasRole('PROCUREMENT_OFFICER') and #request.type.name() == 'INTAKE')")
    public ResponseEntity<StockMovementResponse> recordMovement(
            @Valid @RequestBody RecordMovementRequest request) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        StockMovementResponse response;
        if (request.getType() == MovementType.INTAKE) {
            response = movementService.recordIntake(
                    request, principal.getUserId(), principal.getWarehouseId(), principal.getCompanyId());
        } else {
            response = movementService.recordOutboundSale(
                    request, principal.getUserId(), principal.getWarehouseId(), principal.getCompanyId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List stock movements",
               description = "Returns movements filtered by product, warehouse, type, and date range. ADMIN sees all warehouses; MANAGER sees their assigned warehouse only.")
    @ApiResponse(responseCode = "200", description = "Movement list")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<StockMovementResponse>> listMovements(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String effectiveWarehouseId = "ADMIN".equals(principal.getRole()) ? warehouseId : principal.getWarehouseId();
        return ResponseEntity.ok(movementService.listMovements(productId, effectiveWarehouseId, type, from, to));
    }

    @Operation(summary = "Bulk import intake movements from CSV",
               description = "Imports multiple INTAKE movements for a warehouse from a CSV file (columns: sku, quantity, referenceNumber). Staff must be assigned to the target warehouse.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Movements imported"),
        @ApiResponse(responseCode = "400", description = "CSV parse error or validation failure"),
        @ApiResponse(responseCode = "403", description = "Staff not assigned to this warehouse")
    })
    @PostMapping(value = "/import/{warehouseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public ResponseEntity<List<StockMovementResponse>> importIntake(
            @PathVariable String warehouseId,
            @RequestParam("file") MultipartFile file) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(
                movementService.importIntakeFromCsv(
                        warehouseId, file, principal.getUserId(), principal.getWarehouseId(), principal.getCompanyId()));
    }
}
