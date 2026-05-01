package com.medical.medcore.dto.response;

import com.medical.medcore.entity.enums.TenantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantResponse {
    private Long id;
    private String subdomain;
    private String name;
    private TenantStatus status;
    private String logoUrl;
    private String primaryColor;
    private String subtitle;
    private LocalDateTime createdAt;
}
