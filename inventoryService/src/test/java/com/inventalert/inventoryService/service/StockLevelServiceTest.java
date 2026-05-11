package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.response.StockLevelResponse;
import com.inventalert.inventoryService.exception.ProductNotFoundException;
import com.inventalert.inventoryService.exception.WarehouseNotFoundException;
import com.inventalert.inventoryService.model.Product;
import com.inventalert.inventoryService.model.StockLevel;
import com.inventalert.inventoryService.repository.ProductRepository;
import com.inventalert.inventoryService.repository.StockLevelRepository;
import com.inventalert.inventoryService.repository.WarehouseRepository;
import com.inventalert.inventoryService.service.impl.StockLevelServiceImpl;
import com.inventalert.inventoryService.model.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockLevelServiceTest {

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private StockLevelServiceImpl stockLevelService;

    private Product product;
    private Warehouse warehouse;
    private StockLevel stockLevel;

    @BeforeEach
    void setUp() {
        product = Product.builder().id("p1").name("Widget").sku("SKU-001")
                .unitOfMeasure("units").defaultThreshold(10).isActive(true).build();

        warehouse = Warehouse.builder().id("w1").name("Main").address("Addr")
                .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).isActive(true).build();

        stockLevel = StockLevel.builder()
                .id("sl1").productId("p1").warehouseId("w1")
                .currentStock(100).threshold(10).velocityPerDay(BigDecimal.ZERO).build();
    }

    @Test
    void getOrCreate_returnsExistingStockLevel() {
        when(stockLevelRepository.findByProductIdAndWarehouseId("p1", "w1"))
                .thenReturn(Optional.of(stockLevel));

        StockLevel result = stockLevelService.getOrCreate("p1", "w1");

        assertThat(result.getId()).isEqualTo("sl1");
        verify(stockLevelRepository, never()).save(any());
    }

    @Test
    void getOrCreate_createsNewWhenNotExists() {
        when(stockLevelRepository.findByProductIdAndWarehouseId("p1", "w1"))
                .thenReturn(Optional.empty());
        when(productRepository.findByIdAndIsActiveTrue("p1")).thenReturn(Optional.of(product));
        when(warehouseRepository.findByIdAndIsActiveTrue("w1")).thenReturn(Optional.of(warehouse));
        when(stockLevelRepository.save(any())).thenReturn(stockLevel);

        StockLevel result = stockLevelService.getOrCreate("p1", "w1");

        assertThat(result).isNotNull();
        verify(stockLevelRepository).save(any());
    }

    @Test
    void getOrCreate_throwsProductNotFound() {
        when(stockLevelRepository.findByProductIdAndWarehouseId("bad", "w1"))
                .thenReturn(Optional.empty());
        when(productRepository.findByIdAndIsActiveTrue("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockLevelService.getOrCreate("bad", "w1"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void setWarehouseThreshold_updatesThreshold() {
        when(stockLevelRepository.findByProductIdAndWarehouseId("p1", "w1"))
                .thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(any())).thenReturn(stockLevel);

        stockLevelService.setWarehouseThreshold("p1", "w1", 50);

        assertThat(stockLevel.getThreshold()).isEqualTo(50);
        verify(stockLevelRepository).save(stockLevel);
    }

    @Test
    void getAllStockLevels_returnsMappedResponses() {
        when(stockLevelRepository.findAll()).thenReturn(List.of(stockLevel));

        List<StockLevelResponse> result = stockLevelService.getAllStockLevels();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo("p1");
    }

    @Test
    void getStockForWarehouse_returnsFilteredResponses() {
        when(stockLevelRepository.findByWarehouseId("w1")).thenReturn(List.of(stockLevel));

        List<StockLevelResponse> result = stockLevelService.getStockForWarehouse("w1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWarehouseId()).isEqualTo("w1");
    }
}
