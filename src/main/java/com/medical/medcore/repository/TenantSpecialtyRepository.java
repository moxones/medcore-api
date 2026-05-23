package com.medical.medcore.repository;

import com.medical.medcore.entity.TenantSpecialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantSpecialtyRepository extends JpaRepository<TenantSpecialty, Long> {

    @Query("SELECT ts FROM TenantSpecialty ts JOIN FETCH ts.specialty WHERE ts.tenantId = :tenantId")
    List<TenantSpecialty> findByTenantIdFetchSpecialty(Long tenantId);

    Optional<TenantSpecialty> findByTenantIdAndSpecialty_Id(Long tenantId, Long specialtyId);

    boolean existsByTenantIdAndSpecialty_Id(Long tenantId, Long specialtyId);
}
