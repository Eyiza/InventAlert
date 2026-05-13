package com.inventalert.inventoryService.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SubmitReconciliationRequest {

    @NotBlank(message = "Product is required.")
    private String productId;

    @NotBlank(message = "Warehouse is required.")
    private String warehouseId;

    @Min(value = 0, message = "Physical count must be 0 or greater.")
    private int physicalCount;

    @NotBlank(message = "Reason is required.")
    private String reason;
}
