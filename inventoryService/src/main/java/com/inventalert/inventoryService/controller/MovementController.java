package com.inventalert.inventoryService.controller;

import com.inventalert.inventoryService.dto.request.RecordMovementRequest;
import com.inventalert.inventoryService.dto.response.StockMovementResponse;
import com.inventalert.inventoryService.model.MovementType;
import com.inventalert.inventoryService.security.model.JwtUser;
import com.inventalert.inventoryService.service.MovementService;
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

@RestController
@RequestMapping("/api/movements")
@RequiredArgsConstructor
public class MovementController {

    private final MovementService movementService;

    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
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

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<StockMovementResponse>> listMovements(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(movementService.listMovements(productId, warehouseId, type, from, to));
    }

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
