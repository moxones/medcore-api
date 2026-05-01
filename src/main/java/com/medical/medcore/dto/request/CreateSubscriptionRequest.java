package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateSubscriptionRequest {
    @NotNull
    private Long tenantId;
    @NotNull
    private Long planId;
    @NotNull
    private LocalDate startDate;
    private LocalDate endDate;
    @NotNull
    private String status;
}
