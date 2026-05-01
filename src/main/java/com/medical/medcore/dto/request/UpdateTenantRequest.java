package com.medical.medcore.dto.request;

import com.medical.medcore.entity.enums.TenantStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTenantRequest {
    @NotBlank
    private String name;
    
    private String logoUrl;
    private String primaryColor;
    private String subtitle;
    
    @NotNull
    private TenantStatus status;
}
