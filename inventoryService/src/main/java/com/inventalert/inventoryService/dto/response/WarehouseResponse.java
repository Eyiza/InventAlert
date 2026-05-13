package com.inventalert.inventoryService.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WarehouseResponse {
    private String id;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    // Lombok generates isActive() getter which Jackson serializes as "active" — force the correct key
    @JsonProperty("isActive")
    private boolean isActive;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
