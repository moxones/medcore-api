package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTenantRequest {
    @NotBlank
    private String subdomain;
    
    @NotBlank
    private String name;
    
    private String logoUrl;
    private String primaryColor;
    private String subtitle;
}
