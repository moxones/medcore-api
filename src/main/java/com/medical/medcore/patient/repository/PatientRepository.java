package com.medical.medcore.patient.repository;

import com.medical.medcore.patient.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    List<Patient> findAllByTenantId(Long tenantId);

    @Query(value = "SELECT p FROM Patient p JOIN FETCH p.person WHERE p.tenantId = :tenantId",
            countQuery = "SELECT COUNT(p) FROM Patient p WHERE p.tenantId = :tenantId")
    Page<Patient> findByTenantIdWithPerson(@Param("tenantId") Long tenantId, Pageable pageable);

    Optional<Patient> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByPersonIdAndTenantId(Long personId, Long tenantId);

    Optional<Patient> findByPersonIdAndTenantId(Long personId, Long tenantId);

    @Query("""
            SELECT DISTINCT p FROM Patient p
            JOIN FETCH p.person per
            JOIN PersonDocument pd ON pd.person.id = per.id
            WHERE p.tenantId = :tenantId
              AND pd.documentNumber LIKE :prefix
            ORDER BY p.id DESC
            """)
    List<Patient> searchByDocumentPrefix(@Param("tenantId") Long tenantId, @Param("prefix") String prefix,
                                         Pageable pageable);

    @Query("""
            SELECT DISTINCT p FROM Patient p
            JOIN FETCH p.person per
            LEFT JOIN User u ON u.person.id = per.id AND u.tenantId = p.tenantId
            WHERE p.tenantId = :tenantId
              AND (LOWER(CONCAT(COALESCE(per.firstName, ''), ' ', COALESCE(per.lastName, ''))) LIKE :pattern
                OR LOWER(per.contactEmail) LIKE :pattern
                OR LOWER(u.email) LIKE :pattern
                OR LOWER(p.medicalRecordNumber) LIKE :pattern)
            ORDER BY p.id DESC
            """)
    List<Patient> searchByText(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);
}
