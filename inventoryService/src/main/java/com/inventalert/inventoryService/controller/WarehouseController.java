package com.inventalert.inventoryService.controller;

import com.inventalert.inventoryService.dto.request.CreateWarehouseRequest;
import com.inventalert.inventoryService.dto.request.UpdateWarehouseRequest;
import com.inventalert.inventoryService.dto.response.WarehouseResponse;
import com.inventalert.inventoryService.security.model.JwtUser;
import com.inventalert.inventoryService.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Warehouses", description = "Warehouse registration, location updates, and activation lifecycle")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "Create a warehouse", description = "Registers a new warehouse with GPS coordinates used for transfer distance calculations. Requires ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Warehouse created"),
        @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody CreateWarehouseRequest request) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(warehouseService.create(request, principal.getUserId()));
    }

    @Operation(summary = "List warehouses", description = "Returns all active warehouses for the company. Accessible to all authenticated roles.")
    @ApiResponse(responseCode = "200", description = "Warehouse list")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','WAREHOUSE_STAFF','PROCUREMENT_OFFICER')")
    public ResponseEntity<List<WarehouseResponse>> list() {
        return ResponseEntity.ok(warehouseService.list());
    }

    @Operation(summary = "Update a warehouse", description = "Updates warehouse name, address, or GPS coordinates. Requires ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Warehouse updated"),
        @ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WarehouseResponse> update(@PathVariable String id,
                                                     @Valid @RequestBody UpdateWarehouseRequest request) {
        return ResponseEntity.ok(warehouseService.update(id, request));
    }

    @Operation(summary = "Deactivate a warehouse", description = "Soft-deactivates the warehouse. Existing stock and history are preserved.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deactivated"),
        @ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        warehouseService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivate a warehouse", description = "Re-enables a previously deactivated warehouse.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Reactivated"),
        @ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activate(@PathVariable String id) {
        warehouseService.activate(id);
        return ResponseEntity.noContent().build();
    }
}
