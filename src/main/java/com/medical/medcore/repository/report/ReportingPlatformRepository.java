package com.medical.medcore.repository.report;

import com.medical.medcore.entity.Tenant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Agregaciones de plataforma para SUPER_ADMIN (sin filtro de tenant). */
public interface ReportingPlatformRepository extends Repository<Tenant, Long> {

    /** [activeTenants, totalTenants] estado global de organizaciones. */
    @Query(value = """
            SELECT SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END), COUNT(*)
            FROM tenants
            """, nativeQuery = true)
    List<Object[]> tenantTotals();

    /** Altas de tenants en el rango. */
    @Query(value = """
            SELECT COUNT(*) FROM tenants
            WHERE created_at >= :from AND created_at < :to
            """, nativeQuery = true)
    long tenantSignups(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** [month, count] altas por mes. */
    @Query(value = """
            SELECT to_char(created_at, 'YYYY-MM'), COUNT(*)
            FROM tenants
            WHERE created_at >= :from AND created_at < :to
            GROUP BY 1 ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> signupsByMonth(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** [tenantName, planName, status, createdAt] organizaciones recientes. */
    @Query(value = """
            SELECT te.name,
                   (SELECT pl.name FROM subscriptions su JOIN plans pl ON pl.id = su.plan_id
                      WHERE su.tenant_id = te.id ORDER BY su.start_date DESC LIMIT 1) AS plan,
                   te.status,
                   to_char(te.created_at, 'YYYY-MM-DD')
            FROM tenants te
            ORDER BY te.created_at DESC NULLS LAST
            LIMIT 20
            """, nativeQuery = true)
    List<Object[]> recentOrganizations();

    /** [planName, count, revenue] ingreso por plan (price del plan) de suscripciones vigentes. */
    @Query(value = """
            SELECT pl.name, COUNT(*), SUM(pl.price)
            FROM subscriptions su
            JOIN plans pl ON pl.id = su.plan_id
            GROUP BY pl.name ORDER BY SUM(pl.price) DESC
            """, nativeQuery = true)
    List<Object[]> revenueByPlan();

    /** [planName, tenantName, amount, startDate] suscripciones facturadas. */
    @Query(value = """
            SELECT pl.name, te.name, pl.price, to_char(su.start_date, 'YYYY-MM-DD')
            FROM subscriptions su
            JOIN plans pl ON pl.id = su.plan_id
            JOIN tenants te ON te.id = su.tenant_id
            WHERE su.start_date >= :from AND su.start_date < :to
            ORDER BY su.start_date DESC
            """, nativeQuery = true)
    List<Object[]> billedSubscriptions(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** [status, count] distribución de suscripciones por estado. */
    @Query(value = """
            SELECT COALESCE(status, 'SIN_ESTADO'), COUNT(*)
            FROM subscriptions GROUP BY status
            """, nativeQuery = true)
    List<Object[]> subscriptionStatusCounts();

    /** Suscripciones por vencer (end_date entre hoy y hoy+7). */
    @Query(value = """
            SELECT COUNT(*) FROM subscriptions
            WHERE end_date IS NOT NULL AND end_date >= :today AND end_date <= :limit
            """, nativeQuery = true)
    long expiringSoon(@Param("today") LocalDate today, @Param("limit") LocalDate limit);

    /** Suscripciones vencidas (end_date < hoy). */
    @Query(value = """
            SELECT COUNT(*) FROM subscriptions
            WHERE end_date IS NOT NULL AND end_date < :today
            """, nativeQuery = true)
    long expired(@Param("today") LocalDate today);

    /** [tenantName, planName, status, endDate] detalle de suscripciones. */
    @Query(value = """
            SELECT te.name, pl.name, su.status, to_char(su.end_date, 'YYYY-MM-DD')
            FROM subscriptions su
            JOIN plans pl ON pl.id = su.plan_id
            JOIN tenants te ON te.id = su.tenant_id
            ORDER BY su.end_date ASC NULLS LAST
            """, nativeQuery = true)
    List<Object[]> subscriptionDetail();

    /** [tenantName, appointments] citas creadas por organización en el rango. */
    @Query(value = """
            SELECT te.name, COUNT(*)
            FROM appointments a
            JOIN tenants te ON te.id = a.tenant_id
            WHERE a.created_at >= :from AND a.created_at < :to
            GROUP BY te.name ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> appointmentsByTenant(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Total de citas creadas en el rango (todas las organizaciones). */
    @Query(value = """
            SELECT COUNT(*) FROM appointments a
            WHERE a.created_at >= :from AND a.created_at < :to
            """, nativeQuery = true)
    long totalAppointmentsCreated(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Tenants con al menos una cita creada en el rango. */
    @Query(value = """
            SELECT COUNT(DISTINCT a.tenant_id) FROM appointments a
            WHERE a.created_at >= :from AND a.created_at < :to
            """, nativeQuery = true)
    long tenantsWithActivity(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
