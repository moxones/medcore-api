package com.medical.medcore.repository.report;

import com.medical.medcore.entity.Appointment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportingAppointmentRepository extends Repository<Appointment, Long> {

    @Query(value = """
            SELECT s.code, s.name, COUNT(*)
            FROM appointments a
            JOIN appointment_status s ON s.id = a.status_id
            WHERE a.tenant_id = :tenantId
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
              AND (:doctorId = -1 OR a.doctor_id = :doctorId)
            GROUP BY s.code, s.name
            """, nativeQuery = true)
    List<Object[]> statusBreakdown(@Param("tenantId") Long tenantId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   @Param("applyBranch") boolean applyBranch,
                                   @Param("branchIds") List<Long> branchIds,
                                   @Param("doctorId") Long doctorId);

    @Query(value = """
            SELECT COALESCE(a.flow_status, 'SCHEDULED'), COUNT(*)
            FROM appointments a
            WHERE a.tenant_id = :tenantId
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            GROUP BY a.flow_status
            """, nativeQuery = true)
    List<Object[]> flowBreakdown(@Param("tenantId") Long tenantId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 @Param("applyBranch") boolean applyBranch,
                                 @Param("branchIds") List<Long> branchIds);

    @Query(value = """
            SELECT sp.name, COUNT(*)
            FROM appointments a
            JOIN doctor_specialties ds ON ds.doctor_id = a.doctor_id
            JOIN specialties sp ON sp.id = ds.specialty_id
            WHERE a.tenant_id = :tenantId
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            GROUP BY sp.name
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> specialtyBreakdown(@Param("tenantId") Long tenantId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      @Param("applyBranch") boolean applyBranch,
                                      @Param("branchIds") List<Long> branchIds);

    @Query(value = """
            SELECT to_char(a.scheduled_at, 'YYYY-MM-DD') AS day,
                   COUNT(*) AS total,
                   SUM(CASE WHEN a.flow_status = 'COMPLETED' THEN 1 ELSE 0 END) AS attended,
                   SUM(CASE WHEN s.code ILIKE 'CANCEL%' THEN 1 ELSE 0 END) AS cancelled,
                   SUM(CASE WHEN s.code ILIKE 'NO%SHOW%' THEN 1 ELSE 0 END) AS no_show
            FROM appointments a
            JOIN appointment_status s ON s.id = a.status_id
            WHERE a.tenant_id = :tenantId
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
              AND (:doctorId = -1 OR a.doctor_id = :doctorId)
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> dailyStats(@Param("tenantId") Long tenantId,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to,
                              @Param("applyBranch") boolean applyBranch,
                              @Param("branchIds") List<Long> branchIds,
                              @Param("doctorId") Long doctorId);

    @Query(value = """
            SELECT d.id,
                   TRIM(COALESCE(per.first_name, '') || ' ' || COALESCE(per.last_name, '')) AS doctor_name,
                   COUNT(*) AS total,
                   SUM(CASE WHEN a.flow_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed,
                   SUM(CASE WHEN s.code ILIKE 'NO%SHOW%' THEN 1 ELSE 0 END) AS no_show
            FROM appointments a
            JOIN doctors d ON d.id = a.doctor_id
            JOIN persons per ON per.id = d.person_id
            JOIN appointment_status s ON s.id = a.status_id
            WHERE a.tenant_id = :tenantId
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
              AND (:doctorId = -1 OR a.doctor_id = :doctorId)
            GROUP BY d.id, per.first_name, per.last_name
            ORDER BY total DESC
            """, nativeQuery = true)
    List<Object[]> doctorProductivity(@Param("tenantId") Long tenantId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      @Param("applyBranch") boolean applyBranch,
                                      @Param("branchIds") List<Long> branchIds,
                                      @Param("doctorId") Long doctorId);

    @Query(value = """
            SELECT to_char(a.scheduled_at, 'YYYY-MM-DD') AS day, COUNT(*)
            FROM appointments a
            WHERE a.tenant_id = :tenantId AND a.doctor_id = :doctorId
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
              AND a.flow_status = 'COMPLETED'
            GROUP BY day ORDER BY day
            """, nativeQuery = true)
    List<Object[]> completedByDayForDoctor(@Param("tenantId") Long tenantId,
                                           @Param("doctorId") Long doctorId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT COUNT(DISTINCT a.patient_id)
            FROM appointments a
            WHERE a.tenant_id = :tenantId AND a.doctor_id = :doctorId
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
            """, nativeQuery = true)
    long distinctPatientsForDoctor(@Param("tenantId") Long tenantId,
                                   @Param("doctorId") Long doctorId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT sp.name,
                   COUNT(*) AS total,
                   SUM(CASE WHEN s.code ILIKE 'NO%SHOW%' THEN 1 ELSE 0 END) AS no_show
            FROM appointments a
            JOIN appointment_status s ON s.id = a.status_id
            JOIN doctor_specialties ds ON ds.doctor_id = a.doctor_id
            JOIN specialties sp ON sp.id = ds.specialty_id
            WHERE a.tenant_id = :tenantId AND a.doctor_id = :doctorId
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
            GROUP BY sp.name ORDER BY total DESC
            """, nativeQuery = true)
    List<Object[]> specialtySummaryForDoctor(@Param("tenantId") Long tenantId,
                                             @Param("doctorId") Long doctorId,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT bucket, COUNT(DISTINCT patient_id) FROM (
                SELECT a.patient_id,
                       CASE
                         WHEN date_part('year', age(per.birth_date)) < 18 THEN '0-17'
                         WHEN date_part('year', age(per.birth_date)) < 30 THEN '18-29'
                         WHEN date_part('year', age(per.birth_date)) < 45 THEN '30-44'
                         WHEN date_part('year', age(per.birth_date)) < 60 THEN '45-59'
                         ELSE '60+'
                       END AS bucket
                FROM appointments a
                JOIN patients pt ON pt.id = a.patient_id
                JOIN persons per ON per.id = pt.person_id
                WHERE a.tenant_id = :tenantId
                  AND a.scheduled_at >= :from AND a.scheduled_at < :to
                  AND per.birth_date IS NOT NULL
                  AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            ) g
            GROUP BY bucket
            ORDER BY bucket
            """, nativeQuery = true)
    List<Object[]> ageGroups(@Param("tenantId") Long tenantId,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to,
                             @Param("applyBranch") boolean applyBranch,
                             @Param("branchIds") List<Long> branchIds);

    @Query(value = """
            SELECT COUNT(DISTINCT a.patient_id)
            FROM appointments a
            WHERE a.tenant_id = :tenantId
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            """, nativeQuery = true)
    long distinctPatients(@Param("tenantId") Long tenantId,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          @Param("applyBranch") boolean applyBranch,
                          @Param("branchIds") List<Long> branchIds);

    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT a.patient_id, MIN(a.scheduled_at) AS first_at
                FROM appointments a
                WHERE a.tenant_id = :tenantId
                  AND (:applyBranch = false OR a.branch_id IN (:branchIds))
                GROUP BY a.patient_id
                HAVING MIN(a.scheduled_at) >= :from AND MIN(a.scheduled_at) < :to
            ) n
            """, nativeQuery = true)
    long newPatients(@Param("tenantId") Long tenantId,
                     @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to,
                     @Param("applyBranch") boolean applyBranch,
                     @Param("branchIds") List<Long> branchIds);

    @Query(value = """
            SELECT AVG(date_part('year', age(per.birth_date)))
            FROM appointments a
            JOIN patients pt ON pt.id = a.patient_id
            JOIN persons per ON per.id = pt.person_id
            WHERE a.tenant_id = :tenantId
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
              AND per.birth_date IS NOT NULL
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            """, nativeQuery = true)
    Double averageAge(@Param("tenantId") Long tenantId,
                      @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to,
                      @Param("applyBranch") boolean applyBranch,
                      @Param("branchIds") List<Long> branchIds);

    @Query(value = """
            SELECT b.id, b.name, COUNT(*)
            FROM appointments a
            JOIN branches b ON b.id = a.branch_id
            JOIN appointment_status s ON s.id = a.status_id
            WHERE a.tenant_id = :tenantId
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
              AND s.code NOT ILIKE 'CANCEL%'
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            GROUP BY b.id, b.name
            ORDER BY b.name
            """, nativeQuery = true)
    List<Object[]> occupiedByBranch(@Param("tenantId") Long tenantId,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to,
                                    @Param("applyBranch") boolean applyBranch,
                                    @Param("branchIds") List<Long> branchIds);

    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (COALESCE(a.completed_at, a.finished_at) - a.started_at)) / 60.0)
            FROM appointments a
            WHERE a.tenant_id = :tenantId
              AND a.started_at IS NOT NULL
              AND COALESCE(a.completed_at, a.finished_at) IS NOT NULL
              AND COALESCE(a.completed_at, a.finished_at) >= a.started_at
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
              AND (:doctorId = -1 OR a.doctor_id = :doctorId)
            """, nativeQuery = true)
    Double avgConsultationMinutes(@Param("tenantId") Long tenantId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  @Param("applyBranch") boolean applyBranch,
                                  @Param("branchIds") List<Long> branchIds,
                                  @Param("doctorId") Long doctorId);

    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (a.started_at - a.checked_in_at)) / 60.0),
                   MAX(EXTRACT(EPOCH FROM (a.started_at - a.checked_in_at)) / 60.0),
                   COUNT(*)
            FROM appointments a
            WHERE a.tenant_id = :tenantId
              AND a.checked_in_at IS NOT NULL AND a.started_at IS NOT NULL
              AND a.started_at >= a.checked_in_at
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            """, nativeQuery = true)
    List<Object[]> waitingTimeStats(@Param("tenantId") Long tenantId,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to,
                                    @Param("applyBranch") boolean applyBranch,
                                    @Param("branchIds") List<Long> branchIds);

    @Query(value = """
            SELECT to_char(a.checked_in_at, 'HH24') || ':00' AS slot,
                   AVG(EXTRACT(EPOCH FROM (a.started_at - a.checked_in_at)) / 60.0)
            FROM appointments a
            WHERE a.tenant_id = :tenantId
              AND a.checked_in_at IS NOT NULL AND a.started_at IS NOT NULL
              AND a.started_at >= a.checked_in_at
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            GROUP BY slot ORDER BY slot
            """, nativeQuery = true)
    List<Object[]> waitingTimeByHour(@Param("tenantId") Long tenantId,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to,
                                     @Param("applyBranch") boolean applyBranch,
                                     @Param("branchIds") List<Long> branchIds);
}
