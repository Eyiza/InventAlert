package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.request.CreateWarehouseRequest;
import com.inventalert.inventoryService.dto.request.UpdateWarehouseRequest;
import com.inventalert.inventoryService.dto.response.WarehouseResponse;
import com.inventalert.inventoryService.exception.WarehouseNotFoundException;
import com.inventalert.inventoryService.model.Warehouse;
import com.inventalert.inventoryService.repository.WarehouseRepository;
import com.inventalert.inventoryService.service.impl.WarehouseServiceImpl;
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
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    private Warehouse activeWarehouse;

    @BeforeEach
    void setUp() {
        activeWarehouse = Warehouse.builder()
                .id("w1")
                .name("Main Warehouse")
                .address("123 Main St")
                .latitude(new BigDecimal("40.7128"))
                .longitude(new BigDecimal("-74.0060"))
                .isActive(true)
                .createdBy("user1")
                .build();
    }

    @Test
    void create_savesAndReturnsResponse() {
        CreateWarehouseRequest req = new CreateWarehouseRequest();
        req.setName("Main Warehouse");
        req.setAddress("123 Main St");
        req.setLatitude(new BigDecimal("40.7128"));
        req.setLongitude(new BigDecimal("-74.0060"));

        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(activeWarehouse);

        WarehouseResponse response = warehouseService.create(req, "user1");

        assertThat(response.getName()).isEqualTo("Main Warehouse");
        assertThat(response.getId()).isEqualTo("w1");
        verify(warehouseRepository).save(any(Warehouse.class));
    }

    @Test
    void list_returnsOnlyActiveWarehouses() {
        when(warehouseRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(activeWarehouse));

        List<WarehouseResponse> result = warehouseService.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Main Warehouse");
    }

    @Test
    void update_updatesFieldsAndSaves() {
        UpdateWarehouseRequest req = new UpdateWarehouseRequest();
        req.setName("Updated Name");

        Warehouse updated = Warehouse.builder().id("w1").name("Updated Name")
                .address("123 Main St").latitude(new BigDecimal("40.7128"))
                .longitude(new BigDecimal("-74.0060")).isActive(true).createdBy("user1").build();

        when(warehouseRepository.findByIdAndIsActiveTrue("w1")).thenReturn(Optional.of(activeWarehouse));
        when(warehouseRepository.save(any())).thenReturn(updated);

        WarehouseResponse response = warehouseService.update("w1", req);

        assertThat(response.getName()).isEqualTo("Updated Name");
        verify(warehouseRepository).save(any());
    }

    @Test
    void update_throwsWhenNotFound() {
        when(warehouseRepository.findByIdAndIsActiveTrue("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.update("bad", new UpdateWarehouseRequest()))
                .isInstanceOf(WarehouseNotFoundException.class);
    }

    @Test
    void deactivate_setsIsActiveFalse() {
        when(warehouseRepository.findByIdAndIsActiveTrue("w1")).thenReturn(Optional.of(activeWarehouse));
        when(warehouseRepository.save(any())).thenReturn(activeWarehouse);

        warehouseService.deactivate("w1");

        assertThat(activeWarehouse.isActive()).isFalse();
        verify(warehouseRepository).save(activeWarehouse);
    }

    @Test
    void deactivate_throwsWhenNotFound() {
        when(warehouseRepository.findByIdAndIsActiveTrue("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.deactivate("bad"))
                .isInstanceOf(WarehouseNotFoundException.class);
    }
}
