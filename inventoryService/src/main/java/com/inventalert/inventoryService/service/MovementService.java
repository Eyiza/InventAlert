package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.request.RecordMovementRequest;
import com.inventalert.inventoryService.dto.response.StockMovementResponse;
import com.inventalert.inventoryService.model.MovementType;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public interface MovementService {
    StockMovementResponse recordIntake(RecordMovementRequest request, String userId,
                                       String staffWarehouseId, String companyId);
    StockMovementResponse recordOutboundSale(RecordMovementRequest request, String userId,
                                              String staffWarehouseId, String companyId);
    List<StockMovementResponse> listMovements(String productId, String warehouseId,
                                              MovementType type, LocalDateTime from, LocalDateTime to);
    List<StockMovementResponse> importIntakeFromCsv(String warehouseId, MultipartFile file,
                                                    String userId, String staffWarehouseId, String companyId);
}
