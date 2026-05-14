package com.inventalert.inventoryService.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SetThresholdRequest {

    @Min(value = 0, message = "Threshold must be 0 or greater.")
    private int threshold;
}
