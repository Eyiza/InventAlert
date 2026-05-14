package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.request.RecordMovementRequest;
import com.inventalert.inventoryService.dto.response.StockMovementResponse;
import com.inventalert.inventoryService.exception.CsvImportException;
import com.inventalert.inventoryService.exception.CsvParseException;
import com.inventalert.inventoryService.exception.InsufficientStockException;
import com.inventalert.inventoryService.exception.InvalidMovementTypeException;
import com.inventalert.inventoryService.exception.WarehouseNotAssignedException;
import com.inventalert.inventoryService.kafka.StockMovementProducer;
import com.inventalert.inventoryService.model.*;
import com.inventalert.inventoryService.repository.ProductRepository;
import com.inventalert.inventoryService.repository.StockMovementRepository;
import com.inventalert.inventoryService.service.impl.MovementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    @Mock
    private StockMovementRepository movementRepository;

    @Mock
    private StockLevelService stockLevelService;

    @Mock
    private VelocityCalculationService velocityService;

    @Mock
    private ThresholdCheckService thresholdCheckService;

    @Mock
    private RestockAlertService restockAlertService;

    @Mock
    private StockMovementProducer movementProducer;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private MovementServiceImpl movementService;

    private StockLevel stockLevel;

    @BeforeEach
    void setUp() {
        stockLevel = StockLevel.builder()
                .id("sl1").productId("p1").warehouseId("w1")
                .currentStock(100).threshold(10).velocityPerDay(BigDecimal.ZERO).build();
    }

    @Test
    void recordIntake_incrementsStockAndPublishes() {
        RecordMovementRequest req = new RecordMovementRequest();
        req.setProductId("p1");
        req.setWarehouseId("w1");
        req.setType(MovementType.INTAKE);
        req.setQuantity(50);

        StockMovement saved = StockMovement.builder().id("m1").productId("p1").warehouseId("w1")
                .type(MovementType.INTAKE).quantity(50).createdBy("user1").build();

        when(stockLevelService.getOrCreate("p1", "w1")).thenReturn(stockLevel);
        when(movementRepository.save(any())).thenReturn(saved);

        StockMovementResponse response = movementService.recordIntake(req, "user1", "w1", "company1");

        assertThat(stockLevel.getCurrentStock()).isEqualTo(150);
        assertThat(response.getId()).isEqualTo("m1");
        verify(movementProducer).publishMovementCreated(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void recordIntake_throwsInvalidTypeWhenNotIntake() {
        RecordMovementRequest req = new RecordMovementRequest();
        req.setType(MovementType.OUTBOUND_SALE);
        req.setWarehouseId("w1");

        assertThatThrownBy(() -> movementService.recordIntake(req, "user1", "w1", "company1"))
                .isInstanceOf(InvalidMovementTypeException.class);
    }

    @Test
    void recordIntake_throwsWhenStaffWarehouseMismatch() {
        RecordMovementRequest req = new RecordMovementRequest();
        req.setType(MovementType.INTAKE);
        req.setWarehouseId("w99");

        assertThatThrownBy(() -> movementService.recordIntake(req, "user1", "w1", "company1"))
                .isInstanceOf(WarehouseNotAssignedException.class);
    }

    @Test
    void recordOutboundSale_decrementsStockAndTriggersTasks() {
        RecordMovementRequest req = new RecordMovementRequest();
        req.setProductId("p1");
        req.setWarehouseId("w1");
        req.setType(MovementType.OUTBOUND_SALE);
        req.setQuantity(30);

        StockMovement saved = StockMovement.builder().id("m2").productId("p1").warehouseId("w1")
                .type(MovementType.OUTBOUND_SALE).quantity(30).createdBy("user1").build();

        when(stockLevelService.getOrCreate("p1", "w1")).thenReturn(stockLevel);
        when(movementRepository.save(any())).thenReturn(saved);

        StockMovementResponse response = movementService.recordOutboundSale(req, "user1", "w1", "company1");

        assertThat(stockLevel.getCurrentStock()).isEqualTo(70);
        assertThat(response.getId()).isEqualTo("m2");
        verify(velocityService).recalculate("p1", "w1");
        verify(thresholdCheckService).checkThreshold("p1", "w1", "company1");
    }

    @Test
    void recordOutboundSale_throwsInsufficientStockWhenNotEnough() {
        RecordMovementRequest req = new RecordMovementRequest();
        req.setProductId("p1");
        req.setWarehouseId("w1");
        req.setType(MovementType.OUTBOUND_SALE);
        req.setQuantity(200); // more than currentStock=100

        when(stockLevelService.getOrCreate("p1", "w1")).thenReturn(stockLevel);

        assertThatThrownBy(() -> movementService.recordOutboundSale(req, "user1", "w1", "company1"))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(stockLevel.getCurrentStock()).isEqualTo(100); // unchanged
    }

    @Test
    void recordOutboundSale_throwsWhenStaffWarehouseMismatch() {
        RecordMovementRequest req = new RecordMovementRequest();
        req.setType(MovementType.OUTBOUND_SALE);
        req.setWarehouseId("w99");

        assertThatThrownBy(() -> movementService.recordOutboundSale(req, "user1", "w1", "company1"))
                .isInstanceOf(WarehouseNotAssignedException.class);
    }

    @Test
    void listMovements_delegatesToRepository() {
        StockMovement m = StockMovement.builder().id("m1").productId("p1").warehouseId("w1")
                .type(MovementType.INTAKE).quantity(10).createdBy("u1").build();
        when(movementRepository.findWithFilters(any(), any(), any(), any(), any())).thenReturn(List.of(m));

        List<StockMovementResponse> result = movementService.listMovements("p1", "w1", null, null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void importIntakeFromCsv_throwsWhenStaffAtWrongWarehouse() {
        MockMultipartFile file = new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> movementService.importIntakeFromCsv("w1", file, "user1", "w99", "company1"))
                .isInstanceOf(WarehouseNotAssignedException.class);
    }

    @Test
    void importIntakeFromCsv_throwsCsvParseExceptionOnMalformedFile() throws IOException {
        MockMultipartFile file = mock(MockMultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("disk error"));

        assertThatThrownBy(() -> movementService.importIntakeFromCsv("w1", file, "user1", "w1", "company1"))
                .isInstanceOf(CsvParseException.class);
    }

    @Test
    void importIntakeFromCsv_throwsCsvImportExceptionWhenQuantityIsZero() throws IOException {
        String csv = "sku,quantity,referenceNumber\nSKU-001,0,REF001\n";
        MockMultipartFile file = new MockMultipartFile("file", csv.getBytes(StandardCharsets.UTF_8));

        Product product = Product.builder().id("p1").name("Widget").sku("SKU-001")
                .unitOfMeasure("units").defaultThreshold(10).isActive(true).build();
        when(productRepository.findBySkuAndIsActiveTrue("SKU-001")).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> movementService.importIntakeFromCsv("w1", file, "user1", "w1", "company1"))
                .isInstanceOf(CsvImportException.class);
    }
}
