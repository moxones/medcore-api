package com.medical.medcore.repository;

import com.medical.medcore.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Page<Doctor> findByTenantIdAndIsActiveTrue(Long tenantId, Pageable pageable);

    Optional<Doctor> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Doctor> findByPersonIdAndTenantId(Long personId, Long tenantId);

    @Query(value = """
            SELECT DISTINCT d FROM Doctor d
            JOIN FETCH d.person
            WHERE d.tenantId = :tenantId
            AND (:isActive IS NULL OR d.isActive = :isActive)
            AND (:branchId IS NULL OR EXISTS (
                SELECT db FROM DoctorBranch db
                WHERE db.doctor = d AND db.branch.id = :branchId AND db.isActive = true
            ))
            AND (:specialtyId IS NULL OR EXISTS (
                SELECT ds FROM DoctorSpecialty ds
                WHERE ds.doctor = d AND ds.specialty.id = :specialtyId
            ))
            AND (:todayDayOfWeek IS NULL OR EXISTS (
                SELECT s FROM DoctorSchedule s
                WHERE s.doctor = d AND s.isActive = true
                AND s.dayOfWeek = :todayDayOfWeek
                AND (s.validFrom IS NULL OR s.validFrom <= :today)
                AND (s.validUntil IS NULL OR s.validUntil >= :today)
            ))
            ORDER BY d.id
            """,
            countQuery = """
            SELECT COUNT(DISTINCT d) FROM Doctor d
            WHERE d.tenantId = :tenantId
            AND (:isActive IS NULL OR d.isActive = :isActive)
            AND (:branchId IS NULL OR EXISTS (
                SELECT db FROM DoctorBranch db
                WHERE db.doctor = d AND db.branch.id = :branchId AND db.isActive = true
            ))
            AND (:specialtyId IS NULL OR EXISTS (
                SELECT ds FROM DoctorSpecialty ds
                WHERE ds.doctor = d AND ds.specialty.id = :specialtyId
            ))
            AND (:todayDayOfWeek IS NULL OR EXISTS (
                SELECT s FROM DoctorSchedule s
                WHERE s.doctor = d AND s.isActive = true
                AND s.dayOfWeek = :todayDayOfWeek
                AND (s.validFrom IS NULL OR s.validFrom <= :today)
                AND (s.validUntil IS NULL OR s.validUntil >= :today)
            ))
            """)
    Page<Doctor> findByFiltersForCard(
            @Param("tenantId") Long tenantId,
            @Param("isActive") Boolean isActive,
            @Param("branchId") Long branchId,
            @Param("specialtyId") Long specialtyId,
            @Param("todayDayOfWeek") Integer todayDayOfWeek,
            @Param("today") LocalDate today,
            Pageable pageable);
}
