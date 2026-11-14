package com.medical.medcore.catalog.dto;

import java.time.LocalDateTime;

public record SystemCatalogResponse(
        Long id,
        String code,
        String name,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
