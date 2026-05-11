package com.inventalert.inventoryService.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateProductRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String sku;

    @NotBlank
    private String unitOfMeasure;

    @Min(0)
    private int defaultThreshold;
}
