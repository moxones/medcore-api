package com.medical.medcore.dto.response;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long totalAppointmentsToday,
        long cancelledAppointmentsToday,
        long completedAppointmentsToday,
        BigDecimal totalRevenueThisMonth,
        double noShowRatePercentage
) {}
