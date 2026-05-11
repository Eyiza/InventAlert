package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.response.TransferSuggestionResponse;
import com.inventalert.inventoryService.model.StockLevel;

import java.util.List;

public interface TransferService {
    void createSuggestion(String productId, String deficitWarehouseId,
                          List<StockLevel> candidates, int shortage, String companyId);
    List<TransferSuggestionResponse> list(String role, String warehouseId);
    void approve(String id, String managerId, String companyId);
    void reject(String id, String managerId, String companyId);
    void dispatch(String id, String staffId, String staffWarehouseId, String companyId);
    void accept(String id, String staffId, String staffWarehouseId, String companyId);
    void rejectDelivery(String id, String staffId, String staffWarehouseId, String companyId);
}
