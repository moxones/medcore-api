package com.medical.medcore.repository;

import com.medical.medcore.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    // --- List (with FETCH for lazy associations) ---

    @Query("""
            SELECT ds FROM DoctorSchedule ds
            LEFT JOIN FETCH ds.doctorBranch db LEFT JOIN FETCH db.branch
            LEFT JOIN FETCH ds.branch
            WHERE ds.doctor.id = :doctorId
            AND (:dayOfWeek IS NULL OR ds.dayOfWeek = :dayOfWeek)
            AND (:isActive IS NULL OR ds.isActive = :isActive)
            ORDER BY ds.dayOfWeek, ds.startTime
            """)
    List<DoctorSchedule> findByDoctorIdWithFilters(
            @Param("doctorId") Long doctorId,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("isActive") Boolean isActive);

    // --- Detail ---

    @Query("""
            SELECT ds FROM DoctorSchedule ds
            LEFT JOIN FETCH ds.doctorBranch db LEFT JOIN FETCH db.branch
            LEFT JOIN FETCH ds.branch
            WHERE ds.id = :scheduleId AND ds.doctor.id = :doctorId
            """)
    Optional<DoctorSchedule> findByIdAndDoctorId(
            @Param("scheduleId") Long scheduleId,
            @Param("doctorId") Long doctorId);

    // --- Overlap check: [startTime, endTime) vs existing active schedules ---
    // Two ranges overlap when: existingStart < newEnd AND newStart < existingEnd
    // excludeId is null on create, set to the schedule's own id on update (to skip itself)
    @Query("""
            SELECT COUNT(ds) FROM DoctorSchedule ds
            WHERE ds.doctorBranch.id = :doctorBranchId
            AND ds.dayOfWeek = :dayOfWeek
            AND ds.isActive = true
            AND (:excludeId IS NULL OR ds.id <> :excludeId)
            AND ds.startTime < :endTime
            AND :startTime < ds.endTime
            """)
    long countOverlap(
            @Param("doctorBranchId") Long doctorBranchId,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);

    @Query("""
            SELECT ds FROM DoctorSchedule ds
            LEFT JOIN FETCH ds.doctorBranch db LEFT JOIN FETCH db.branch
            LEFT JOIN FETCH ds.branch
            WHERE ds.doctor.id = :doctorId
            AND ds.isActive = true
            AND ds.dayOfWeek = :dayOfWeek
            AND (db.branch.id = :branchId OR ds.branch.id = :branchId)
            ORDER BY ds.startTime
            """)
    List<DoctorSchedule> findActiveByDoctorIdAndBranchIdAndDay(
            @Param("doctorId") Long doctorId,
            @Param("branchId") Long branchId,
            @Param("dayOfWeek") Integer dayOfWeek);

    // --- Batch load for card endpoint ---

    @Query("SELECT ds FROM DoctorSchedule ds LEFT JOIN FETCH ds.branch LEFT JOIN FETCH ds.doctorBranch db LEFT JOIN FETCH db.branch WHERE ds.doctor.id IN :doctorIds AND ds.isActive = true")
    List<DoctorSchedule> findActiveByDoctorIdIn(@Param("doctorIds") List<Long> doctorIds);

    // --- Legacy: kept for findActiveByDoctorIdAndBranchId used by DoctorScheduleService ---

    @Query("""
            SELECT ds FROM DoctorSchedule ds
            LEFT JOIN FETCH ds.doctorBranch db LEFT JOIN FETCH db.branch
            LEFT JOIN FETCH ds.branch b
            WHERE ds.doctor.id = :doctorId AND ds.isActive = true
            AND (db.branch.id = :branchId OR b.id = :branchId)
            """)
    List<DoctorSchedule> findActiveByDoctorIdAndBranchId(
            @Param("doctorId") Long doctorId,
            @Param("branchId") Long branchId);
}
