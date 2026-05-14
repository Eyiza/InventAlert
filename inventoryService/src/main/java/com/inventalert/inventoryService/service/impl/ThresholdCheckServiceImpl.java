package com.inventalert.inventoryService.service.impl;

import com.inventalert.inventoryService.model.AlertStatus;
import com.inventalert.inventoryService.model.StockLevel;
import com.inventalert.inventoryService.model.TransferStatus;
import com.inventalert.inventoryService.repository.RestockAlertRepository;
import com.inventalert.inventoryService.repository.StockLevelRepository;
import com.inventalert.inventoryService.repository.TransferSuggestionRepository;
import com.inventalert.inventoryService.service.RestockAlertService;
import com.inventalert.inventoryService.service.ThresholdCheckService;
import com.inventalert.inventoryService.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThresholdCheckServiceImpl implements ThresholdCheckService {

    private final StockLevelRepository stockLevelRepository;
    private final RestockAlertRepository restockAlertRepository;
    private final TransferSuggestionRepository transferSuggestionRepository;
    private final RestockAlertService restockAlertService;
    private final TransferService transferService;

    @Override
    public void checkThreshold(String productId, String warehouseId, String companyId) {
        StockLevel level = stockLevelRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow();

        if (level.getCurrentStock() >= level.getThreshold()) return;

        if (restockAlertRepository.existsByProductIdAndWarehouseIdAndStatus(
                productId, warehouseId, AlertStatus.OPEN)) return;

        int shortage = level.getThreshold() - level.getCurrentStock();

        List<StockLevel> candidates = stockLevelRepository
                .findByProductIdAndWarehouseIdNot(productId, warehouseId)
                .stream()
                .filter(c -> (c.getCurrentStock() - c.getThreshold()) >= shortage)
                .toList();

        if (candidates.isEmpty()) {
            restockAlertService.createAlert(
                    productId, warehouseId, level.getCurrentStock(), level.getThreshold(), companyId);
        } else {
            if (!transferSuggestionRepository.existsByProductIdAndToWarehouseIdAndStatusIn(
                    productId, warehouseId, List.of(TransferStatus.SUGGESTED, TransferStatus.APPROVED))) {
                transferService.createSuggestion(productId, warehouseId, candidates, shortage, companyId);
            }
        }
    }
}
