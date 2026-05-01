package com.medical.medcore.repository;

import com.medical.medcore.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    List<Patient> findAllByTenantId(Long tenantId);

    Optional<Patient> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByPersonIdAndTenantId(Long personId, Long tenantId);

    Optional<Patient> findByPersonIdAndTenantId(Long personId, Long tenantId);

    @Query("""
            SELECT p FROM Patient p
            JOIN p.person per
            JOIN PersonDocument pd ON pd.person.id = per.id
            WHERE p.tenantId = :tenantId
            AND pd.documentNumber = :term
            """)
    List<Patient> searchByDocument(Long tenantId, String term);

    @Query("""
            SELECT p FROM Patient p
            JOIN p.person per
            LEFT JOIN User u ON u.person.id = per.id AND u.tenantId = p.tenantId
            WHERE p.tenantId = :tenantId
            AND (
                LOWER(per.firstName) LIKE CONCAT('%', :term, '%') OR
                LOWER(per.lastName) LIKE CONCAT('%', :term, '%') OR
                LOWER(per.contactEmail) LIKE CONCAT('%', :term, '%') OR
                LOWER(u.email) LIKE CONCAT('%', :term, '%')
            )
            """)
    List<Patient> searchByText(Long tenantId, String term);
}