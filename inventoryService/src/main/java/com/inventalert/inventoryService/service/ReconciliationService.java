package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.request.SubmitReconciliationRequest;
import com.inventalert.inventoryService.dto.response.ReconciliationResponse;

import java.util.List;

public interface ReconciliationService {
    ReconciliationResponse submit(SubmitReconciliationRequest request, String staffId, String companyId);
    List<ReconciliationResponse> list();
    void approve(String id, String managerId);
    void reject(String id, String managerId);
}
