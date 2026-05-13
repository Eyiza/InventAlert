package com.inventalert.inventoryService.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StaffInitiateTransferRequest {

    @NotBlank(message = "Product is required.")
    private String productId;

    @NotBlank(message = "Source warehouse is required.")
    private String fromWarehouseId;

    @NotBlank(message = "Destination warehouse is required.")
    private String toWarehouseId;

    @Min(value = 1, message = "Quantity must be at least 1.")
    private int quantity;
}
