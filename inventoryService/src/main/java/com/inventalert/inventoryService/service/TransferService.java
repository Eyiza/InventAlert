package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.request.StaffInitiateTransferRequest;
import com.inventalert.inventoryService.dto.response.TransferSuggestionResponse;
import com.inventalert.inventoryService.model.StockLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransferService {
    void createSuggestion(String productId, String deficitWarehouseId,
                          List<StockLevel> candidates, int shortage, String companyId);
    TransferSuggestionResponse initiateByStaff(StaffInitiateTransferRequest request,
                                               String staffId, String companyId);
    Page<TransferSuggestionResponse> list(String role, String warehouseId, Pageable pageable);
    void approve(String id, String managerId, String companyId);
    void reject(String id, String managerId, String companyId);
    void dispatch(String id, String staffId, String staffWarehouseId, String companyId);
    void accept(String id, String staffId, String staffWarehouseId, String companyId);
    void rejectDelivery(String id, String staffId, String staffWarehouseId, String companyId);
}
