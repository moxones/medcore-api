package com.medical.medcore.identity.repository;

import com.medical.medcore.identity.domain.PersonDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonDocumentRepository extends JpaRepository<PersonDocument, Long> {

    Optional<PersonDocument> findByTenantIdAndDocumentType_IdAndDocumentNumber(
            Long tenantId, Long documentTypeId, String documentNumber);

    Optional<PersonDocument> findFirstByPersonIdOrderByIdAsc(Long personId);
}
