package com.inventalert.inventoryService.service.impl;

import com.inventalert.inventoryService.dto.request.CreateProductRequest;
import com.inventalert.inventoryService.dto.request.UpdateProductRequest;
import com.inventalert.inventoryService.dto.response.CsvImportErrorResponse;
import com.inventalert.inventoryService.dto.response.ProductResponse;
import com.inventalert.inventoryService.exception.CsvImportException;
import com.inventalert.inventoryService.exception.DuplicateSkuException;
import com.inventalert.inventoryService.exception.ProductNotFoundException;
import com.inventalert.inventoryService.model.Product;
import com.inventalert.inventoryService.repository.ProductRepository;
import com.inventalert.inventoryService.service.ProductService;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request, String createdBy) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }
        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .unitOfMeasure(request.getUnitOfMeasure())
                .defaultThreshold(request.getDefaultThreshold())
                .isActive(true)
                .createdBy(createdBy)
                .build();
        return toResponse(productRepository.save(product));
    }

    @Override
    public List<ProductResponse> list() {
        return productRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductResponse update(String id, UpdateProductRequest request) {
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (request.getName() != null)             product.setName(request.getName());
        if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
            if (productRepository.existsBySku(request.getSku())) throw new DuplicateSkuException(request.getSku());
            product.setSku(request.getSku());
        }
        if (request.getUnitOfMeasure() != null)    product.setUnitOfMeasure(request.getUnitOfMeasure());
        if (request.getDefaultThreshold() != null) product.setDefaultThreshold(request.getDefaultThreshold());

        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void setDefaultThreshold(String id, int threshold) {
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setDefaultThreshold(threshold);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public List<ProductResponse> importFromCsv(MultipartFile file, String createdBy) {
        List<ProductCsvRow> rows;
        try (Reader reader = new InputStreamReader(file.getInputStream())) {
            rows = new CsvToBeanBuilder<ProductCsvRow>(reader)
                    .withType(ProductCsvRow.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV file", e);
        }

        List<CsvImportErrorResponse.RowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            ProductCsvRow row = rows.get(i);
            int rowNum = i + 1;
            if (row.getName() == null || row.getName().isBlank()) {
                errors.add(new CsvImportErrorResponse.RowError(rowNum, "name is required"));
            } else if (row.getSku() == null || row.getSku().isBlank()) {
                errors.add(new CsvImportErrorResponse.RowError(rowNum, "sku is required"));
            } else if (productRepository.existsBySku(row.getSku())) {
                errors.add(new CsvImportErrorResponse.RowError(rowNum, "SKU already exists: " + row.getSku()));
            } else if (row.getDefaultThreshold() < 0) {
                errors.add(new CsvImportErrorResponse.RowError(rowNum, "defaultThreshold must be >= 0"));
            }
        }

        if (!errors.isEmpty()) {
            throw new CsvImportException(rows.size(), errors);
        }

        return rows.stream().map(row -> {
            Product product = Product.builder()
                    .name(row.getName())
                    .sku(row.getSku())
                    .unitOfMeasure(row.getUnitOfMeasure())
                    .defaultThreshold(row.getDefaultThreshold())
                    .isActive(true)
                    .createdBy(createdBy)
                    .build();
            return toResponse(productRepository.save(product));
        }).toList();
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .sku(p.getSku())
                .unitOfMeasure(p.getUnitOfMeasure())
                .defaultThreshold(p.getDefaultThreshold())
                .isActive(p.isActive())
                .createdBy(p.getCreatedBy())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    @Data
    public static class ProductCsvRow {
        @CsvBindByName(column = "name", required = true)
        private String name;

        @CsvBindByName(column = "sku", required = true)
        private String sku;

        @CsvBindByName(column = "unitOfMeasure", required = true)
        private String unitOfMeasure;

        @CsvBindByName(column = "defaultThreshold")
        private int defaultThreshold = 0;
    }
}
