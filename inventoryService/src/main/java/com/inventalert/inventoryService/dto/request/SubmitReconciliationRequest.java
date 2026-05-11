package com.inventalert.inventoryService.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SubmitReconciliationRequest {

    @NotBlank
    private String productId;

    @NotBlank
    private String warehouseId;

    @Min(0)
    private int physicalCount;

    @NotBlank
    private String reason;
}
