package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.request.CreateProductRequest;
import com.inventalert.inventoryService.dto.request.UpdateProductRequest;
import com.inventalert.inventoryService.dto.response.ProductResponse;
import com.inventalert.inventoryService.exception.DuplicateSkuException;
import com.inventalert.inventoryService.exception.ProductNotFoundException;
import com.inventalert.inventoryService.model.Product;
import com.inventalert.inventoryService.repository.ProductRepository;
import com.inventalert.inventoryService.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product activeProduct;

    @BeforeEach
    void setUp() {
        activeProduct = Product.builder()
                .id("p1")
                .name("Widget A")
                .sku("SKU-001")
                .unitOfMeasure("units")
                .defaultThreshold(10)
                .isActive(true)
                .createdBy("user1")
                .build();
    }

    @Test
    void create_savesAndReturnsResponse() {
        CreateProductRequest req = new CreateProductRequest();
        req.setName("Widget A");
        req.setSku("SKU-001");
        req.setUnitOfMeasure("units");
        req.setDefaultThreshold(10);

        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(activeProduct);

        ProductResponse response = productService.create(req, "user1");

        assertThat(response.getSku()).isEqualTo("SKU-001");
        assertThat(response.getName()).isEqualTo("Widget A");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_throwsDuplicateSkuExceptionWhenSkuExists() {
        CreateProductRequest req = new CreateProductRequest();
        req.setSku("SKU-001");

        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(req, "user1"))
                .isInstanceOf(DuplicateSkuException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void list_returnsOnlyActiveProducts() {
        when(productRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(activeProduct));

        List<ProductResponse> result = productService.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("SKU-001");
    }

    @Test
    void update_updatesFieldsAndSaves() {
        UpdateProductRequest req = new UpdateProductRequest();
        req.setName("Updated Widget");

        when(productRepository.findByIdAndIsActiveTrue("p1")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenReturn(activeProduct);

        ProductResponse response = productService.update("p1", req);

        assertThat(response).isNotNull();
        verify(productRepository).save(any());
    }

    @Test
    void update_throwsWhenNotFound() {
        when(productRepository.findByIdAndIsActiveTrue("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update("bad", new UpdateProductRequest()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void setDefaultThreshold_updatesThresholdAndSaves() {
        when(productRepository.findByIdAndIsActiveTrue("p1")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenReturn(activeProduct);

        productService.setDefaultThreshold("p1", 50);

        assertThat(activeProduct.getDefaultThreshold()).isEqualTo(50);
        verify(productRepository).save(activeProduct);
    }

    @Test
    void setDefaultThreshold_throwsWhenNotFound() {
        when(productRepository.findByIdAndIsActiveTrue("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.setDefaultThreshold("bad", 10))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
