package com.inventalert.inventoryService.dto.request;

import com.inventalert.inventoryService.model.MovementType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RecordMovementRequest {

    @NotBlank
    private String productId;

    @NotBlank
    private String warehouseId;

    @NotNull
    private MovementType type;

    @Min(1)
    private int quantity;

    private String referenceNumber;
}
