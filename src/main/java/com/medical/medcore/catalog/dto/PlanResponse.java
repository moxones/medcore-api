package com.medical.medcore.catalog.dto;

import com.medical.medcore.platform.domain.Plan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlanResponse(
        Long id,
        String name,
        String code,
        BigDecimal price,
        Integer maxUsers,
        Integer maxBranches,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(plan.getId(), plan.getName(), plan.getCode(), plan.getPrice(), plan.getMaxUsers(),
                plan.getMaxBranches(), !Boolean.FALSE.equals(plan.getIsActive()), plan.getCreatedAt(),
                plan.getUpdatedAt());
    }
}
