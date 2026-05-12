package com.inventalert.identityService.dto.request;

import lombok.Data;

@Data
public class UpdateCompanyRequest {
    private String companyName;
    private String logoUrl;
}
