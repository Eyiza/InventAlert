package com.inventalert.inventoryService.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateProductRequest {

    @Size(min = 1)
    private String name;

    @Size(min = 1)
    private String unitOfMeasure;

    @Min(0)
    private Integer defaultThreshold;
}
