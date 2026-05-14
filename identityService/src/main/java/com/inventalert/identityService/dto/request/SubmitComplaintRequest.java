package com.inventalert.identityService.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmitComplaintRequest {
    private String subject;

    @NotBlank(message = "Please describe your issue.")
    private String description;
}
