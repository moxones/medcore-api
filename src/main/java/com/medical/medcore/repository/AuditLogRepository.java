package com.medical.medcore.repository;

import com.medical.medcore.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTenantIdAndTableNameAndRecordIdOrderByChangedAtDescIdDesc(
            Long tenantId, String tableName, Long recordId);
}
