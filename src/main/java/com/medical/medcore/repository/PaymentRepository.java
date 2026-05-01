package com.medical.medcore.repository;

import com.medical.medcore.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.appointment.tenantId = :tenantId AND " +
           "p.status = 'COMPLETED' AND p.paymentDate >= :startDate AND p.paymentDate < :endDate")
    BigDecimal sumCompletedPaymentsByTenantAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
