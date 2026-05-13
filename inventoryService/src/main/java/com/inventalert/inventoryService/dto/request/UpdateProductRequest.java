package com.inventalert.inventoryService.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateProductRequest {

    @Size(min = 1, message = "Product name cannot be empty.")
    private String name;

    @Size(min = 1, message = "Unit of measure cannot be empty.")
    private String unitOfMeasure;

    @Min(value = 0, message = "Threshold must be 0 or greater.")
    private Integer defaultThreshold;
}
