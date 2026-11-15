package com.medical.medcore.platform.service;

import com.medical.medcore.platform.domain.ClinicModule;
import com.medical.medcore.platform.domain.TenantProcessConfig;
import com.medical.medcore.platform.domain.ClinicProcess;
import com.medical.medcore.platform.repository.TenantProcessConfigRepository;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TenantProcessConfigService {

    private final TenantProcessConfigRepository repository;
    private final TenantModuleService moduleService;

    public Map<ClinicProcess, Boolean> getConfig() {
        Long tenantId = TenantContext.requireTenantId();
        return resolve(tenantId);
    }

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

    private Map<ClinicProcess, Boolean> resolve(Long tenantId) {
        Map<ClinicProcess, Boolean> result = new EnumMap<>(ClinicProcess.class);
        for (ClinicProcess process : ClinicProcess.values()) {
            result.put(process, process.activeByDefault());
        }
        for (TenantProcessConfig config : repository.findByTenantId(tenantId)) {
            try {
                result.put(ClinicProcess.valueOf(config.getProcess()), Boolean.TRUE.equals(config.getIsActive()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        Set<ClinicModule> modules = moduleService.enabled(tenantId);
        result.replaceAll((process, active) -> active && process.availableWith(modules));
        return result;
    }
}
