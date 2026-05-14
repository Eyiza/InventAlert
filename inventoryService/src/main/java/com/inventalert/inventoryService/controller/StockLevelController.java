package com.inventalert.inventoryService.controller;

import com.inventalert.inventoryService.dto.request.SetThresholdRequest;
import com.inventalert.inventoryService.dto.response.StockLevelResponse;
import com.inventalert.inventoryService.security.model.JwtUser;
import com.inventalert.inventoryService.service.StockLevelService;
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

@RestController
@RequiredArgsConstructor
public class StockLevelController {

    private final StockLevelService stockLevelService;

    @GetMapping("/api/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<StockLevelResponse>> getAllStock(
            @PageableDefault(size = 20, sort = "currentStock", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(stockLevelService.getAllStockLevels(pageable));
    }

    @GetMapping("/api/stock/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','WAREHOUSE_STAFF','PROCUREMENT_OFFICER')")
    public ResponseEntity<List<StockLevelResponse>> getStockForWarehouse(@PathVariable String warehouseId) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!"ADMIN".equals(principal.getRole()) && !warehouseId.equals(principal.getWarehouseId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(stockLevelService.getStockForWarehouse(warehouseId));
    }

    @PatchMapping("/api/stock-levels/{productId}/{warehouseId}/threshold")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setThreshold(@PathVariable String productId,
                                              @PathVariable String warehouseId,
                                              @Valid @RequestBody SetThresholdRequest request) {
        stockLevelService.setWarehouseThreshold(productId, warehouseId, request.getThreshold());
        return ResponseEntity.noContent().build();
    }
}
