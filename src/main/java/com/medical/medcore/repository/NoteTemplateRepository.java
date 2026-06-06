package com.medical.medcore.repository;

import com.medical.medcore.entity.NoteTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteTemplateRepository extends JpaRepository<NoteTemplate, Long> {

    List<NoteTemplate> findByTenantIdAndDoctorIdOrderByUpdatedAtDescIdDesc(Long tenantId, Long doctorId);

    Optional<NoteTemplate> findByIdAndTenantIdAndDoctorId(Long id, Long tenantId, Long doctorId);
}
