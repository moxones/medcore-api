package com.medical.medcore.audit.controller;

import com.medical.medcore.audit.dto.AuditLogResponse;
import com.medical.medcore.audit.domain.AuditLog;
import com.medical.medcore.audit.repository.AuditLogRepository;
import com.medical.medcore.shared.security.annotation.RequireAdmin;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/audit-log")
@RequiredArgsConstructor
@RequireAdmin
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> findForRecord(
            @RequestParam String tableName,
            @RequestParam Long recordId) {
        Long tenantId = TenantContext.requireTenantId();
        List<AuditLogResponse> rows = auditLogRepository
                .findByTenantIdAndTableNameAndRecordIdOrderByChangedAtDescIdDesc(tenantId, tableName, recordId)
                .stream()
                .map(this::map)
                .toList();
        return ResponseEntity.ok(new ApiResponse<>(true, rows, "Bitácora de auditoría"));
    }

    private AuditLogResponse map(AuditLog a) {
        return new AuditLogResponse(
                a.getId(), a.getTableName(), a.getRecordId(), a.getAction(),
                a.getOldValues(), a.getNewValues(), a.getChangedBy(), a.getChangedAt(), a.getIpAddress());
    }
}
