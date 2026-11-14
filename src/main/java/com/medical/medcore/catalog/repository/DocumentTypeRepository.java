package com.medical.medcore.catalog.repository;

import com.medical.medcore.catalog.domain.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

    Optional<DocumentType> findByCode(String code);
    List<DocumentType> findByIsActiveTrue();
    boolean existsByCode(String code);
}
