package com.inventalert.identityService.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @NotBlank
    private String companyName;

    @NotBlank
    @Email
    private String adminEmail;

    @NotBlank
    @Size(min = 8)
    private String password;

    private String logoUrl;
}
