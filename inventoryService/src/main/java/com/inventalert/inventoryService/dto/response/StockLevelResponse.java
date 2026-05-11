package com.inventalert.inventoryService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StockLevelResponse {
    private String id;
    private String productId;
    private String warehouseId;
    private int currentStock;
    private int threshold;
    private BigDecimal velocityPerDay;
    private Integer daysUntilEmpty;
}
