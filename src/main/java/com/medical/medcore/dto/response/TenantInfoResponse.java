package com.medical.medcore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class TenantInfoResponse {
    private String name;
    private String logoUrl;
    private String primaryColor;
    private String subtitle;
}