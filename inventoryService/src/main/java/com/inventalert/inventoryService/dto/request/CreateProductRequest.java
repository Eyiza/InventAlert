package com.inventalert.inventoryService.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateProductRequest {

    @NotBlank(message = "Product name is required.")
    private String name;

    @NotBlank(message = "SKU is required.")
    private String sku;

    @NotBlank(message = "Unit of measure is required.")
    private String unitOfMeasure;

    @Min(value = 0, message = "Threshold must be 0 or greater.")
    private int defaultThreshold;
}
