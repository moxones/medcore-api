package com.medical.medcore.repository;

import com.medical.medcore.entity.PersonDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonDocumentRepository extends JpaRepository<PersonDocument, Long> {

    Optional<PersonDocument> findByDocumentTypeIdAndDocumentNumber(Long typeId, String number);

    boolean existsByPersonId(Long personId);
}