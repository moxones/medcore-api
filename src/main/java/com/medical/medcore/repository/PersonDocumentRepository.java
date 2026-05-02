package com.medical.medcore.repository;

import com.medical.medcore.entity.PersonDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PersonDocumentRepository extends JpaRepository<PersonDocument, Long> {

    @Query("SELECT pd FROM PersonDocument pd JOIN pd.person p WHERE pd.documentType.id = :typeId AND pd.documentNumber = :number AND p.tenantId = :tenantId")
    Optional<PersonDocument> findByDocumentTypeIdAndDocumentNumberAndTenantId(Long typeId, String number, Long tenantId);

    boolean existsByPersonId(Long personId);

    @Query("SELECT CASE WHEN COUNT(pd) > 0 THEN true ELSE false END FROM PersonDocument pd JOIN pd.person p WHERE pd.documentType.id = :documentTypeId AND pd.documentNumber = :documentNumber AND p.tenantId = :tenantId")
    boolean existsByDocumentTypeIdAndDocumentNumberAndTenantId(Long documentTypeId, String documentNumber, Long tenantId);
}