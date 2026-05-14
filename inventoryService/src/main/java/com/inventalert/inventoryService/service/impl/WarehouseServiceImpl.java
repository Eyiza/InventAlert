package com.inventalert.inventoryService.service.impl;

import com.inventalert.inventoryService.dto.request.CreateWarehouseRequest;
import com.inventalert.inventoryService.dto.request.UpdateWarehouseRequest;
import com.inventalert.inventoryService.dto.response.WarehouseResponse;
import com.inventalert.inventoryService.exception.WarehouseNotFoundException;
import com.inventalert.inventoryService.model.Warehouse;
import com.inventalert.inventoryService.repository.WarehouseRepository;
import com.inventalert.inventoryService.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public WarehouseResponse create(CreateWarehouseRequest request, String createdBy) {
        Warehouse warehouse = Warehouse.builder()
                .name(request.getName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isActive(true)
                .createdBy(createdBy)
                .build();
        return toResponse(warehouseRepository.save(warehouse));
    }

    @Override
    public List<WarehouseResponse> list() {
        return warehouseRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public WarehouseResponse update(String id, UpdateWarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new WarehouseNotFoundException(id));

        if (request.getName() != null)      warehouse.setName(request.getName());
        if (request.getAddress() != null)   warehouse.setAddress(request.getAddress());
        if (request.getLatitude() != null)  warehouse.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) warehouse.setLongitude(request.getLongitude());

        return toResponse(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional
    public void deactivate(String id) {
        Warehouse warehouse = warehouseRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new WarehouseNotFoundException(id));
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
    }

    @Override
    @Transactional
    public void activate(String id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(id));
        warehouse.setActive(true);
        warehouseRepository.save(warehouse);
    }

    private WarehouseResponse toResponse(Warehouse w) {
        return WarehouseResponse.builder()
                .id(w.getId())
                .name(w.getName())
                .address(w.getAddress())
                .latitude(w.getLatitude())
                .longitude(w.getLongitude())
                .isActive(w.isActive())
                .createdBy(w.getCreatedBy())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }
}
