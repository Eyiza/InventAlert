package com.inventalert.inventoryService.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SetThresholdRequest {

    @Min(0)
    private int threshold;
}
