package com.inventalert.inventoryService.dto.request;

import com.inventalert.inventoryService.model.MovementType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RecordMovementRequest {

    @NotBlank(message = "Product is required.")
    private String productId;

    @NotBlank(message = "Warehouse is required.")
    private String warehouseId;

    @NotNull(message = "Movement type is required.")
    private MovementType type;

    @Min(value = 1, message = "Quantity must be at least 1.")
    private int quantity;

    private String referenceNumber;
}
