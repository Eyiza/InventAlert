package com.inventalert.inventoryService.service.impl;

import com.inventalert.inventoryService.dto.request.RecordMovementRequest;
import com.inventalert.inventoryService.dto.response.CsvImportErrorResponse;
import com.inventalert.inventoryService.dto.response.StockMovementResponse;
import com.inventalert.inventoryService.exception.*;
import com.inventalert.inventoryService.kafka.StockMovementProducer;
import com.inventalert.inventoryService.model.*;
import com.inventalert.inventoryService.repository.ProductRepository;
import com.inventalert.inventoryService.repository.StockMovementRepository;
import com.inventalert.inventoryService.service.*;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovementServiceImpl implements MovementService {

    private final StockMovementRepository movementRepository;
    private final StockLevelService stockLevelService;
    private final VelocityCalculationService velocityService;
    private final ThresholdCheckService thresholdCheckService;
    private final StockMovementProducer movementProducer;
    private final ProductRepository productRepository;

    @Override
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public StockMovementResponse recordIntake(RecordMovementRequest request, String userId,
                                               String staffWarehouseId, String companyId) {
        if (request.getType() != MovementType.INTAKE) {
            throw new InvalidMovementTypeException(request.getType().name());
        }
        if (!request.getWarehouseId().equals(staffWarehouseId)) {
            throw new WarehouseNotAssignedException(request.getWarehouseId());
        }

        StockLevel level = stockLevelService.getOrCreate(request.getProductId(), request.getWarehouseId());

        StockMovement movement = StockMovement.builder()
                .productId(request.getProductId())
                .warehouseId(request.getWarehouseId())
                .type(MovementType.INTAKE)
                .quantity(request.getQuantity())
                .referenceId(request.getReferenceNumber())
                .createdBy(userId)
                .build();
        StockMovement saved = movementRepository.save(movement);

        level.setCurrentStock(level.getCurrentStock() + request.getQuantity());

        movementProducer.publishMovementCreated(
                companyId, saved.getId(), request.getProductId(),
                request.getWarehouseId(), MovementType.INTAKE, request.getQuantity());

        return toResponse(saved);
    }

    @Recover
    StockMovementResponse recoverRecordIntake(ObjectOptimisticLockingFailureException ex,
                                               RecordMovementRequest request, String userId,
                                               String staffWarehouseId, String companyId) {
        throw new StockConflictException();
    }

    @Override
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public StockMovementResponse recordOutboundSale(RecordMovementRequest request, String userId,
                                                     String staffWarehouseId, String companyId) {
        if (request.getType() != MovementType.OUTBOUND_SALE) {
            throw new InvalidMovementTypeException(request.getType().name());
        }
        if (!request.getWarehouseId().equals(staffWarehouseId)) {
            throw new WarehouseNotAssignedException(request.getWarehouseId());
        }

        StockLevel level = stockLevelService.getOrCreate(request.getProductId(), request.getWarehouseId());

        if (level.getCurrentStock() < request.getQuantity()) {
            throw new InsufficientStockException(level.getCurrentStock(), request.getQuantity());
        }

        StockMovement movement = StockMovement.builder()
                .productId(request.getProductId())
                .warehouseId(request.getWarehouseId())
                .type(MovementType.OUTBOUND_SALE)
                .quantity(request.getQuantity())
                .referenceId(request.getReferenceNumber())
                .createdBy(userId)
                .build();
        StockMovement saved = movementRepository.save(movement);

        level.setCurrentStock(level.getCurrentStock() - request.getQuantity());

        movementProducer.publishMovementCreated(
                companyId, saved.getId(), request.getProductId(),
                request.getWarehouseId(), MovementType.OUTBOUND_SALE, request.getQuantity());

        velocityService.recalculate(request.getProductId(), request.getWarehouseId());
        thresholdCheckService.checkThreshold(request.getProductId(), request.getWarehouseId(), companyId);

        return toResponse(saved);
    }

    @Recover
    StockMovementResponse recoverRecordOutboundSale(ObjectOptimisticLockingFailureException ex,
                                                     RecordMovementRequest request, String userId,
                                                     String staffWarehouseId, String companyId) {
        throw new StockConflictException();
    }

    @Override
    public List<StockMovementResponse> listMovements(String productId, String warehouseId,
                                                      MovementType type, LocalDateTime from, LocalDateTime to) {
        return movementRepository.findWithFilters(productId, warehouseId, type, from, to)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public List<StockMovementResponse> importIntakeFromCsv(String warehouseId, MultipartFile file,
                                                            String userId, String staffWarehouseId, String companyId) {
        if (!warehouseId.equals(staffWarehouseId)) {
            throw new WarehouseNotAssignedException(warehouseId);
        }

        List<IntakeCsvRow> rows;
        try (Reader reader = new InputStreamReader(file.getInputStream())) {
            rows = new CsvToBeanBuilder<IntakeCsvRow>(reader)
                    .withType(IntakeCsvRow.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build().parse();
        } catch (Exception e) {
            throw new CsvParseException("Failed to parse CSV file. Please check the file format and try again.", e);
        }

        List<CsvImportErrorResponse.RowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            IntakeCsvRow row = rows.get(i);
            int rowNum = i + 1;
            if (row.getSku() == null || row.getSku().isBlank()) {
                errors.add(new CsvImportErrorResponse.RowError(rowNum, "sku is required"));
            } else if (!productRepository.findBySkuAndIsActiveTrue(row.getSku()).isPresent()) {
                errors.add(new CsvImportErrorResponse.RowError(rowNum, "Product not found for SKU: " + row.getSku()));
            } else if (row.getQuantity() <= 0) {
                errors.add(new CsvImportErrorResponse.RowError(rowNum, "quantity must be > 0"));
            }
        }

        if (!errors.isEmpty()) {
            throw new CsvImportException(rows.size(), errors);
        }

        List<StockMovementResponse> results = new ArrayList<>();
        for (IntakeCsvRow row : rows) {
            Product product = productRepository.findBySkuAndIsActiveTrue(row.getSku()).orElseThrow();
            StockLevel level = stockLevelService.getOrCreate(product.getId(), warehouseId);

            StockMovement movement = StockMovement.builder()
                    .productId(product.getId()).warehouseId(warehouseId)
                    .type(MovementType.INTAKE).quantity(row.getQuantity())
                    .referenceId(row.getReferenceNumber()).createdBy(userId).build();
            StockMovement saved = movementRepository.save(movement);
            level.setCurrentStock(level.getCurrentStock() + row.getQuantity());

            movementProducer.publishMovementCreated(
                    companyId, saved.getId(), product.getId(), warehouseId, MovementType.INTAKE, row.getQuantity());
            results.add(toResponse(saved));
        }
        return results;
    }

    @Recover
    List<StockMovementResponse> recoverImportIntakeFromCsv(ObjectOptimisticLockingFailureException ex,
                                                            String warehouseId, MultipartFile file,
                                                            String userId, String staffWarehouseId, String companyId) {
        throw new StockConflictException();
    }

    private StockMovementResponse toResponse(StockMovement m) {
        return StockMovementResponse.builder()
                .id(m.getId()).productId(m.getProductId()).warehouseId(m.getWarehouseId())
                .type(m.getType().name()).quantity(m.getQuantity())
                .referenceId(m.getReferenceId()).createdBy(m.getCreatedBy())
                .createdAt(m.getCreatedAt())
                .build();
    }

    @Data
    public static class IntakeCsvRow {
        @CsvBindByName(column = "sku", required = true)
        private String sku;

        @CsvBindByName(column = "quantity", required = true)
        private int quantity;

        @CsvBindByName(column = "referenceNumber")
        private String referenceNumber;
    }
}
