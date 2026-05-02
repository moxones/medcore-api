package com.medical.medcore.repository;

import com.medical.medcore.entity.AppointmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentTypeRepository extends JpaRepository<AppointmentType, Long> {
    List<AppointmentType> findAllByTenantId(Long tenantId);
    Optional<AppointmentType> findByIdAndTenantId(Long id, Long tenantId);
}
