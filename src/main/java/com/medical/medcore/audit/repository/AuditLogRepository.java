package com.medical.medcore.audit.repository;

import com.medical.medcore.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTenantIdAndTableNameAndRecordIdOrderByChangedAtDescIdDesc(
            Long tenantId, String tableName, Long recordId);
}
