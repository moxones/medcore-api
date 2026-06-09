package com.medical.medcore.repository.report;

import com.medical.medcore.entity.Payment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Agregaciones de cobros para reportería (SQL nativo). El monto cobrado considera pagos
 * {@code status = 'COMPLETED'}; el pendiente, el resto.
 */
public interface ReportingPaymentRepository extends Repository<Payment, Long> {

    /** [method, operations, total] cobros COMPLETED por método de pago. */
    @Query(value = """
            SELECT COALESCE(p.payment_method, 'Sin método'), COUNT(*), SUM(p.amount)
            FROM payments p
            JOIN appointments a ON a.id = p.appointment_id
            WHERE a.tenant_id = :tenantId AND p.status = 'COMPLETED'
              AND p.payment_date >= :from AND p.payment_date < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            GROUP BY p.payment_method
            ORDER BY SUM(p.amount) DESC
            """, nativeQuery = true)
    List<Object[]> byMethod(@Param("tenantId") Long tenantId,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to,
                            @Param("applyBranch") boolean applyBranch,
                            @Param("branchIds") List<Long> branchIds);

    /** [day, operations, total] cobros COMPLETED por día. */
    @Query(value = """
            SELECT to_char(p.payment_date, 'YYYY-MM-DD'), COUNT(*), SUM(p.amount)
            FROM payments p
            JOIN appointments a ON a.id = p.appointment_id
            WHERE a.tenant_id = :tenantId AND p.status = 'COMPLETED'
              AND p.payment_date >= :from AND p.payment_date < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            GROUP BY 1 ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> byDay(@Param("tenantId") Long tenantId,
                         @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to,
                         @Param("applyBranch") boolean applyBranch,
                         @Param("branchIds") List<Long> branchIds);

    /** [branchName, total] ingresos COMPLETED por sucursal. */
    @Query(value = """
            SELECT b.name, SUM(p.amount)
            FROM payments p
            JOIN appointments a ON a.id = p.appointment_id
            JOIN branches b ON b.id = a.branch_id
            WHERE a.tenant_id = :tenantId AND p.status = 'COMPLETED'
              AND p.payment_date >= :from AND p.payment_date < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            GROUP BY b.name ORDER BY SUM(p.amount) DESC
            """, nativeQuery = true)
    List<Object[]> byBranch(@Param("tenantId") Long tenantId,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to,
                            @Param("applyBranch") boolean applyBranch,
                            @Param("branchIds") List<Long> branchIds);

    /** Total cobrado (COMPLETED) en el rango. */
    @Query(value = """
            SELECT COALESCE(SUM(p.amount), 0)
            FROM payments p
            JOIN appointments a ON a.id = p.appointment_id
            WHERE a.tenant_id = :tenantId AND p.status = 'COMPLETED'
              AND p.payment_date >= :from AND p.payment_date < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            """, nativeQuery = true)
    BigDecimal totalCollected(@Param("tenantId") Long tenantId,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to,
                              @Param("applyBranch") boolean applyBranch,
                              @Param("branchIds") List<Long> branchIds);

    /** Nº de operaciones COMPLETED en el rango. */
    @Query(value = """
            SELECT COUNT(*)
            FROM payments p
            JOIN appointments a ON a.id = p.appointment_id
            WHERE a.tenant_id = :tenantId AND p.status = 'COMPLETED'
              AND p.payment_date >= :from AND p.payment_date < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            """, nativeQuery = true)
    long countCollected(@Param("tenantId") Long tenantId,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to,
                        @Param("applyBranch") boolean applyBranch,
                        @Param("branchIds") List<Long> branchIds);

    /** Monto pendiente de cobro (pagos no COMPLETED) en el rango. */
    @Query(value = """
            SELECT COALESCE(SUM(p.amount), 0)
            FROM payments p
            JOIN appointments a ON a.id = p.appointment_id
            WHERE a.tenant_id = :tenantId AND p.status <> 'COMPLETED'
              AND p.payment_date >= :from AND p.payment_date < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            """, nativeQuery = true)
    BigDecimal totalPending(@Param("tenantId") Long tenantId,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to,
                            @Param("applyBranch") boolean applyBranch,
                            @Param("branchIds") List<Long> branchIds);

    /** [doctorId, revenue] ingresos COMPLETED atribuidos al médico de la cita. */
    @Query(value = """
            SELECT a.doctor_id, SUM(p.amount)
            FROM payments p
            JOIN appointments a ON a.id = p.appointment_id
            WHERE a.tenant_id = :tenantId AND p.status = 'COMPLETED'
              AND p.payment_date >= :from AND p.payment_date < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            GROUP BY a.doctor_id
            """, nativeQuery = true)
    List<Object[]> revenueByDoctor(@Param("tenantId") Long tenantId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   @Param("applyBranch") boolean applyBranch,
                                   @Param("branchIds") List<Long> branchIds);
}
