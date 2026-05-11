package com.inventalert.inventoryService.service;

public interface ThresholdCheckService {
    void checkThreshold(String productId, String warehouseId, String companyId);
}
