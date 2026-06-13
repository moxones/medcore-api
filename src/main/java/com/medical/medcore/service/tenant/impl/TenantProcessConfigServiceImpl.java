package com.medical.medcore.service.tenant.impl;

import com.medical.medcore.entity.TenantProcessConfig;
import com.medical.medcore.entity.enums.ClinicProcess;
import com.medical.medcore.repository.TenantProcessConfigRepository;
import com.medical.medcore.service.tenant.TenantProcessConfigService;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TenantProcessConfigServiceImpl implements TenantProcessConfigService {

    private final TenantProcessConfigRepository repository;

    @Override
    public Map<ClinicProcess, Boolean> getConfig() {
        Long tenantId = TenantContext.requireTenantId();
        return resolve(tenantId);
    }

    @Override
    @Transactional
    public Map<ClinicProcess, Boolean> updateConfig(Map<ClinicProcess, Boolean> processes) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.getCurrentUserId();

        if (processes != null) {
            processes.forEach((process, active) -> {
                if (process == null || active == null) return;
                TenantProcessConfig config = repository
                        .findByTenantIdAndProcess(tenantId, process.name())
                        .orElseGet(() -> TenantProcessConfig.builder()
                                .tenantId(tenantId)
                                .process(process.name())
                                .createdBy(userId)
                                .build());
                config.setIsActive(active);
                config.setUpdatedBy(userId);
                repository.save(config);
            });
        }

        return resolve(tenantId);
    }

    /** Parte de "todos activos" y aplica los overrides persistidos del tenant. */
    private Map<ClinicProcess, Boolean> resolve(Long tenantId) {
        Map<ClinicProcess, Boolean> result = new EnumMap<>(ClinicProcess.class);
        for (ClinicProcess process : ClinicProcess.values()) {
            result.put(process, true);
        }
        for (TenantProcessConfig config : repository.findByTenantId(tenantId)) {
            try {
                result.put(ClinicProcess.valueOf(config.getProcess()), Boolean.TRUE.equals(config.getIsActive()));
            } catch (IllegalArgumentException ignored) {
                // Proceso desconocido (config legada): se ignora.
            }
        }
        return result;
    }
}
