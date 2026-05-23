package com.medical.medcore.repository;

import com.medical.medcore.entity.TenantAppointmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantAppointmentTypeRepository extends JpaRepository<TenantAppointmentType, Long> {

    @Query("SELECT tat FROM TenantAppointmentType tat JOIN FETCH tat.appointmentType WHERE tat.tenantId = :tenantId")
    List<TenantAppointmentType> findByTenantIdFetchType(Long tenantId);

    Optional<TenantAppointmentType> findByTenantIdAndAppointmentType_Id(Long tenantId, Long appointmentTypeId);

    boolean existsByTenantIdAndAppointmentType_Id(Long tenantId, Long appointmentTypeId);
}
