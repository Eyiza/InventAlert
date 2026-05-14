package com.inventalert.inventoryService.controller;

import com.inventalert.inventoryService.dto.request.SetThresholdRequest;
import com.inventalert.inventoryService.dto.response.StockLevelResponse;
import com.inventalert.inventoryService.security.model.JwtUser;
import com.inventalert.inventoryService.service.StockLevelService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Stock Levels", description = "Query current stock positions and configure per-warehouse reorder thresholds")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class StockLevelController {

    private final StockLevelService stockLevelService;

    @Operation(summary = "Get all stock levels (paginated)",
               description = "Returns a paginated view of stock levels across all warehouses and products. Includes days-until-empty estimate based on 7-day velocity. ADMIN only.")
    @ApiResponse(responseCode = "200", description = "Paginated stock level list")
    @GetMapping("/api/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<StockLevelResponse>> getAllStock(
            @PageableDefault(size = 20, sort = "currentStock", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(stockLevelService.getAllStockLevels(pageable));
    }

    @Operation(summary = "Get stock levels for a warehouse",
               description = "Returns all product stock levels for a specific warehouse, sorted by current stock ascending. Staff and procurement officers can only view their assigned warehouse.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock level list"),
        @ApiResponse(responseCode = "403", description = "Access denied — warehouse not assigned to user")
    })
    @GetMapping("/api/stock/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','WAREHOUSE_STAFF','PROCUREMENT_OFFICER')")
    public ResponseEntity<List<StockLevelResponse>> getStockForWarehouse(@PathVariable String warehouseId) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!"ADMIN".equals(principal.getRole()) && !warehouseId.equals(principal.getWarehouseId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(stockLevelService.getStockForWarehouse(warehouseId));
    }

    @Operation(summary = "Set per-warehouse reorder threshold",
               description = "Overrides the product's default threshold for a specific warehouse. Threshold determines when the system triggers a restock alert or transfer suggestion.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Threshold updated"),
        @ApiResponse(responseCode = "404", description = "Stock level record not found")
    })
    @PatchMapping("/api/stock-levels/{productId}/{warehouseId}/threshold")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setThreshold(@PathVariable String productId,
                                              @PathVariable String warehouseId,
                                              @Valid @RequestBody SetThresholdRequest request) {
        stockLevelService.setWarehouseThreshold(productId, warehouseId, request.getThreshold());
        return ResponseEntity.noContent().build();
    }
}
