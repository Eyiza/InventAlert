package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.request.SubmitReconciliationRequest;
import com.inventalert.inventoryService.dto.response.ReconciliationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReconciliationService {
    ReconciliationResponse submit(SubmitReconciliationRequest request, String staffId, String companyId);
    Page<ReconciliationResponse> list(Pageable pageable);
    void approve(String id, String managerId);
    void reject(String id, String managerId);
}
