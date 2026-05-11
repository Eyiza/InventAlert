package com.inventalert.inventoryService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TransferSuggestionResponse {
    private String id;
    private String productId;
    private String fromWarehouseId;
    private String toWarehouseId;
    private int quantity;
    private BigDecimal distanceKm;
    private String distanceSource;
    private String status;
    private String approvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
