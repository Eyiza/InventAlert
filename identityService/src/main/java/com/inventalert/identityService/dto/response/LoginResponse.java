package com.inventalert.identityService.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private String token;
    private String userId;
    private String email;
    private String companyId;
    private String companyName;
    private String role;
    private String warehouseId;
}
