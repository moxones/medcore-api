package com.medical.medcore.service.dashboard;

import com.medical.medcore.dto.response.DashboardSummaryResponse;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.repository.PaymentRepository;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;

    private static final Long STATUS_CANCELLED = 4L;
    private static final Long STATUS_COMPLETED = 3L;

    public DashboardSummaryResponse getSummaryMetrics() {
        Long tenantId = TenantContext.requireTenantId();
        
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.plusDays(1).atStartOfDay();

        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDateTime startOfMonthTime = startOfMonth.atStartOfDay();

        long totalToday = appointmentRepository.countByTenantAndDateRange(tenantId, startOfToday, endOfToday);
        long cancelledToday = appointmentRepository.countByTenantAndStatusAndDateRange(tenantId, STATUS_CANCELLED, startOfToday, endOfToday);
        long completedToday = appointmentRepository.countByTenantAndStatusAndDateRange(tenantId, STATUS_COMPLETED, startOfToday, endOfToday);

        BigDecimal revenueThisMonth = paymentRepository.sumCompletedPaymentsByTenantAndDateRange(tenantId, startOfMonthTime, endOfToday);
        if (revenueThisMonth == null) {
            revenueThisMonth = BigDecimal.ZERO;
        }

        double noShowRate = 0.0;
        if (totalToday > 0) {
            noShowRate = BigDecimal.valueOf((double) cancelledToday / totalToday * 100)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new DashboardSummaryResponse(
                totalToday,
                cancelledToday,
                completedToday,
                revenueThisMonth,
                noShowRate
        );
    }

    public java.util.List<com.medical.medcore.dto.response.DoctorProductivityResponse> getDoctorProductivity() {
        Long tenantId = TenantContext.requireTenantId();
        LocalDate today = LocalDate.now();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.plusMonths(1).withDayOfMonth(1).atStartOfDay();

        return appointmentRepository.getProductivityByDoctor(tenantId, startOfMonth, endOfMonth);
    }
}
