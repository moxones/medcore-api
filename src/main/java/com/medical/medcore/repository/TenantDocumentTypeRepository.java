package com.medical.medcore.repository;

import com.medical.medcore.entity.TenantDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantDocumentTypeRepository extends JpaRepository<TenantDocumentType, Long> {

    @Query("SELECT tdt FROM TenantDocumentType tdt JOIN FETCH tdt.documentType WHERE tdt.tenantId = :tenantId")
    List<TenantDocumentType> findByTenantIdFetchType(Long tenantId);

    Optional<TenantDocumentType> findByTenantIdAndDocumentType_Id(Long tenantId, Long documentTypeId);

    boolean existsByTenantIdAndDocumentType_Id(Long tenantId, Long documentTypeId);
}
