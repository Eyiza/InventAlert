package com.inventalert.inventoryService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StockMovementResponse {
    private String id;
    private String productId;
    private String warehouseId;
    private String type;
    private int quantity;
    private String referenceId;
    private String createdBy;
    private LocalDateTime createdAt;
}
