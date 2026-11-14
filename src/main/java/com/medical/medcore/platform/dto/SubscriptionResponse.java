package com.medical.medcore.platform.dto;

import com.medical.medcore.platform.domain.Plan;
import com.medical.medcore.platform.domain.Subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SubscriptionResponse(
        Long id,
        Long tenantId,
        PlanInfo plan,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        LocalDateTime createdAt
) {

    public record PlanInfo(Long id, String name, String code, BigDecimal price, Integer maxUsers,
                           Integer maxBranches, Boolean isActive) {
    }

    public static SubscriptionResponse from(Subscription subscription) {
        Plan plan = subscription.getPlan();
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getTenantId(),
                new PlanInfo(plan.getId(), plan.getName(), plan.getCode(), plan.getPrice(), plan.getMaxUsers(),
                        plan.getMaxBranches(), plan.getIsActive()),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getStatus(),
                subscription.getCreatedAt());
    }
}
