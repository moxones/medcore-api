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

    @Query(value = """
            SELECT COALESCE(p.payment_method, 'OTHER'), COUNT(*), COALESCE(SUM(p.amount), 0)
            FROM payments p
            JOIN appointments a ON a.id = p.appointment_id
            WHERE a.tenant_id = :tenantId AND p.status = 'COMPLETED'
              AND p.payment_date >= :from AND p.payment_date < :to
              AND (:restricted = false OR a.branch_id IN (:branchIds))
            GROUP BY 1
            ORDER BY 3 DESC
            """, nativeQuery = true)
    List<Object[]> collectedByMethod(@Param("tenantId") Long tenantId,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to,
                                     @Param("restricted") boolean restricted,
                                     @Param("branchIds") List<Long> branchIds);

    @Query(value = """
            SELECT COUNT(*), COALESCE(SUM(GREATEST(COALESCE(a.price, 0) - COALESCE(paid.total, 0), 0)), 0)
            FROM appointments a
            LEFT JOIN (
                SELECT appointment_id, SUM(amount) AS total
                FROM payments
                WHERE status = 'COMPLETED'
                GROUP BY appointment_id
            ) paid ON paid.appointment_id = a.id
            WHERE a.tenant_id = :tenantId AND a.flow_status = 'PENDING_PAYMENT'
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
              AND (:restricted = false OR a.branch_id IN (:branchIds))
            """, nativeQuery = true)
    List<Object[]> pendingBalance(@Param("tenantId") Long tenantId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  @Param("restricted") boolean restricted,
                                  @Param("branchIds") List<Long> branchIds);
}
