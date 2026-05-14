package com.inventalert.identityService.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResolveComplaintRequest {
    @NotBlank(message = "Please provide a resolution note.")
    private String resolution;
}
