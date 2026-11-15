package com.medical.medcore.platform.service;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.medcore.platform.domain.Tenant;
import com.medical.medcore.platform.domain.TenantStatus;
import com.medical.medcore.platform.repository.TenantRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDataService {

    private static final String ROWS_OF_TABLE =
            "SELECT COALESCE(json_agg(t), '[]'::json)::text FROM public.%s t WHERE t.tenant_id = ?";
    private static final String DELETE_FROM_TABLE = "DELETE FROM public.%s WHERE tenant_id = ?";
    private static final String TAG_AUDIT_WITH_TENANT = "SELECT set_config('app.current_tenant_id', ?, true)";

    private final TenantRepository tenantRepository;
    private final TenantDirectory directory;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public Tenant requireTenant(Long id) {
        return tenantRepository.findById(id).orElseThrow(() -> new NotFoundException("Organización no encontrada"));
    }

    @Transactional(readOnly = true)
    public void export(Long id, OutputStream output) {
        Tenant tenant = requireTenant(id);
        try (JsonGenerator json = objectMapper.getFactory().createGenerator(output, JsonEncoding.UTF8)) {
            json.writeStartObject();
            json.writeStringField("format", "medcore-tenant-export/1");
            json.writeStringField("exportedAt", OffsetDateTime.now().toString());
            json.writeFieldName("tenant");
            json.writeRawValue(jdbcTemplate.queryForObject(
                    "SELECT row_to_json(t)::text FROM public.tenants t WHERE t.id = ?", String.class, id));
            json.writeObjectFieldStart("tables");
            for (String table : TenantTables.forExport()) {
                json.writeFieldName(table);
                json.writeRawValue(jdbcTemplate.queryForObject(ROWS_OF_TABLE.formatted(table), String.class, id));
            }
            json.writeEndObject();
            json.writeEndObject();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        log.info("Exportación completa de la organización '{}'", tenant.getSubdomain());
    }

    @Transactional
    public void purge(Long id, String confirmation) {
        Tenant tenant = requireTenant(id);
        if (directory.isPlatformSubdomain(tenant.getSubdomain())) {
            throw new BadRequestException("No se puede eliminar la organización de la plataforma");
        }
        if (tenant.getStatus() != TenantStatus.INACTIVE) {
            throw new ConflictException("TENANT_MUST_BE_INACTIVE",
                    "Desactiva la organización antes de eliminarla definitivamente");
        }
        if (confirmation == null || !confirmation.trim().equalsIgnoreCase(tenant.getSubdomain())) {
            throw new BadRequestException("PURGE_CONFIRMATION_MISMATCH",
                    "Escribe el subdominio de la organización para confirmar la eliminación");
        }
        jdbcTemplate.queryForObject(TAG_AUDIT_WITH_TENANT, String.class, id.toString());
        for (String table : TenantTables.forDeletion()) {
            jdbcTemplate.update(DELETE_FROM_TABLE.formatted(table), id);
        }
        jdbcTemplate.update("DELETE FROM public.tenants WHERE id = ?", id);
        directory.evict(id);
        log.warn("Organización '{}' eliminada definitivamente", tenant.getSubdomain());
    }
}
