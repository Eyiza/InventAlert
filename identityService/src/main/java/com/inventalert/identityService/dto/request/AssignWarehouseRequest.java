package com.inventalert.identityService.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AssignWarehouseRequest(
        @NotBlank(message = "Please select a warehouse.") String warehouseId
) {}
