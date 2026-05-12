package com.inventalert.inventoryService.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StaffInitiateTransferRequest {

    @NotBlank
    private String productId;

    @NotBlank
    private String fromWarehouseId;

    @NotBlank
    private String toWarehouseId;

    @Min(1)
    private int quantity;
}
