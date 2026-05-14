package com.inventalert.inventoryService.controller;

import com.inventalert.inventoryService.dto.request.CreateProductRequest;
import com.inventalert.inventoryService.dto.request.SetThresholdRequest;
import com.inventalert.inventoryService.dto.request.UpdateProductRequest;
import com.inventalert.inventoryService.dto.response.ProductResponse;
import com.inventalert.inventoryService.security.model.JwtUser;
import com.inventalert.inventoryService.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Products", description = "Product catalogue management — CRUD and CSV bulk import")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Create a product", description = "Registers a new product in the catalogue. SKU must be unique. Requires ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created"),
        @ApiResponse(responseCode = "409", description = "Duplicate SKU"),
        @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(request, principal.getUserId()));
    }

    @Operation(summary = "List active products", description = "Returns all active products for the company, ordered by name.")
    @ApiResponse(responseCode = "200", description = "Product list")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductResponse>> list() {
        return ResponseEntity.ok(productService.list());
    }

    @Operation(summary = "Update a product", description = "Updates name, description, unit of measure, or category. Requires ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> update(@PathVariable String id,
                                                   @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @Operation(summary = "Set default reorder threshold", description = "Sets the company-wide default reorder threshold for a product. Per-warehouse thresholds override this.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Threshold updated"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PatchMapping("/{id}/threshold")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setThreshold(@PathVariable String id,
                                              @Valid @RequestBody SetThresholdRequest request) {
        productService.setDefaultThreshold(id, request.getThreshold());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Bulk import products from CSV",
               description = "Imports products from a CSV file (columns: name, sku, unitOfMeasure, defaultThreshold). Skips duplicates. Requires ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Products imported"),
        @ApiResponse(responseCode = "400", description = "CSV parse error")
    })
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductResponse>> importProducts(
            @RequestParam("file") MultipartFile file) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.importFromCsv(file, principal.getUserId()));
    }
}
