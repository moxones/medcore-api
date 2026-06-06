package com.medical.medcore.repository;

import com.medical.medcore.entity.MedicalOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, Long> {
    List<MedicalOrder> findByMedicalEntryIdOrderByIdAsc(Long medicalEntryId);
    List<MedicalOrder> findByMedicalEntryIdInOrderByIdAsc(List<Long> medicalEntryIds);

    @Query("""
            SELECT o FROM MedicalOrder o
            JOIN o.medicalEntry e JOIN e.medicalRecord mr JOIN mr.patient p
            WHERE o.id = :id AND p.tenantId = :tenantId
            """)
    Optional<MedicalOrder> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("""
            SELECT o FROM MedicalOrder o
            JOIN o.medicalEntry e JOIN e.appointment ap
            JOIN e.medicalRecord mr JOIN mr.patient p JOIN p.person per
            WHERE p.tenantId = :tenantId AND ap.doctor.id = :doctorId
            AND (:status IS NULL OR o.status = :status)
            ORDER BY o.id DESC
            """)
    List<MedicalOrder> findByDoctor(@Param("tenantId") Long tenantId,
                                    @Param("doctorId") Long doctorId,
                                    @Param("status") String status);
}
