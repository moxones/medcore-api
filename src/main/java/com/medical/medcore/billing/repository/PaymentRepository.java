package com.medical.medcore.billing.repository;

import com.medical.medcore.billing.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByAppointmentIdOrderByIdDesc(Long appointmentId);

    Optional<Payment> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.appointment.id = :appointmentId AND p.status = com.medical.medcore.billing.domain.PaymentStatus.COMPLETED")
    BigDecimal sumCompletedByAppointment(@Param("appointmentId") Long appointmentId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.tenantId = :tenantId AND " +
           "p.status = com.medical.medcore.billing.domain.PaymentStatus.COMPLETED AND " +
           "p.paymentDate >= :startDate AND p.paymentDate < :endDate")
    BigDecimal sumCompletedPaymentsByTenantAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
