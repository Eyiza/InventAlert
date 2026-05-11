package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.request.CreateWarehouseRequest;
import com.inventalert.inventoryService.dto.request.UpdateWarehouseRequest;
import com.inventalert.inventoryService.dto.response.WarehouseResponse;

import java.util.List;

public interface WarehouseService {
    WarehouseResponse create(CreateWarehouseRequest request, String createdBy);
    List<WarehouseResponse> list();
    WarehouseResponse update(String id, UpdateWarehouseRequest request);
    void deactivate(String id);
}
