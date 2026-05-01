package com.medical.medcore.repository;

import com.medical.medcore.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Page<Doctor> findByTenantIdAndIsActiveTrue(Long tenantId, Pageable pageable);
    Optional<Doctor> findByIdAndTenantId(Long id, Long tenantId);
}
