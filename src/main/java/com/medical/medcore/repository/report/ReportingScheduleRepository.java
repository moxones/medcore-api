package com.medical.medcore.repository.report;

import com.medical.medcore.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReportingScheduleRepository extends Repository<DoctorSchedule, Long> {

    @Query(value = """
            SELECT b.id, b.name, ds.day_of_week, ds.start_time, ds.end_time,
                   ds.slot_duration_minutes, COALESCE(ds.max_patients_per_slot, 1)
            FROM doctor_schedules ds
            JOIN branches b ON b.id = ds.branch_id
            WHERE b.tenant_id = :tenantId
              AND COALESCE(ds.is_active, true) = true
              AND (ds.valid_from IS NULL OR ds.valid_from <= :to)
              AND (ds.valid_until IS NULL OR ds.valid_until >= :from)
              AND (:applyBranch = false OR b.id IN (:branchIds))
            """, nativeQuery = true)
    List<Object[]> activeSchedules(@Param("tenantId") Long tenantId,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to,
                                   @Param("applyBranch") boolean applyBranch,
                                   @Param("branchIds") List<Long> branchIds);
}
