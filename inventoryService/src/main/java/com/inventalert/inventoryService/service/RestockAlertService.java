package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.response.RestockAlertResponse;
import com.inventalert.inventoryService.model.RestockAlert;

import java.util.List;

public interface RestockAlertService {
    RestockAlert createAlert(String productId, String warehouseId, int stockAtAlert, int threshold, String companyId);
    List<RestockAlertResponse> list();
    void acknowledge(String id, String userId);
    void markOrderPlaced(String id, String userId);
    void resolve(String id, String userId);
}
