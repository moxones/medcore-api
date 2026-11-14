package com.medical.medcore.catalog.dto;

import java.math.BigDecimal;

public record CatalogItemResponse(
        Long id,
        String code,
        String name,
        String description,
        Integer durationMinutes,
        Boolean masterActive,
        boolean activated,
        Boolean tenantActive,
        Long tenantLinkId,
        BigDecimal price
) {

    public CatalogItemResponse(Long id, String code, String name, String description, Integer durationMinutes,
                               Boolean masterActive, boolean activated, Boolean tenantActive, Long tenantLinkId) {
        this(id, code, name, description, durationMinutes, masterActive, activated, tenantActive, tenantLinkId, null);
    }
}
