package com.medical.medcore.platform.service;

import com.medical.medcore.platform.domain.ClinicModule;
import com.medical.medcore.platform.domain.TenantModule;
import com.medical.medcore.platform.repository.TenantModuleRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ForbiddenException;
import com.medical.medcore.shared.security.RoleConstants;
import com.medical.medcore.shared.tenancy.AfterTransaction;
import com.medical.medcore.shared.tenancy.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TenantModuleService {

    public static final String CODE_MODULE_DISABLED = "MODULE_DISABLED";

    private static final Map<String, ClinicModule> ROLE_MODULES = Map.of(
            RoleConstants.ASSISTANT, ClinicModule.TRIAGE,
            RoleConstants.PATIENT, ClinicModule.PATIENT_PORTAL);

    private final TenantModuleRepository repository;
    private final Cache<Long, Set<ClinicModule>> cache;

    public TenantModuleService(TenantModuleRepository repository,
                               @Value("${app.tenancy.cache-seconds:30}") long cacheSeconds) {
        this.repository = repository;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(cacheSeconds))
                .maximumSize(10_000)
                .build();
    }

    public Set<ClinicModule> enabled(Long tenantId) {
        Set<ClinicModule> modules = cache.get(tenantId,
                id -> TenantContext.runAsSystem(() -> toSet(repository.findByTenantId(id))));
        return modules.isEmpty() ? EnumSet.noneOf(ClinicModule.class) : EnumSet.copyOf(modules);
    }

    public Map<Long, Set<ClinicModule>> enabledByTenant(Collection<Long> tenantIds) {
        return repository.findByTenantIdIn(tenantIds).stream()
                .collect(Collectors.groupingBy(TenantModule::getTenantId,
                        Collectors.mapping(TenantModule::getModule,
                                Collectors.toCollection(() -> EnumSet.noneOf(ClinicModule.class)))));
    }

    public Set<ClinicModule> current() {
        return enabled(TenantContext.requireTenantId());
    }

    public boolean isEnabled(ClinicModule module) {
        return enabled(TenantContext.requireTenantId()).contains(module);
    }

    public void require(ClinicModule module) {
        if (!isEnabled(module)) {
            throw disabled(module);
        }
    }

    public ForbiddenException disabled(ClinicModule module) {
        return new ForbiddenException(CODE_MODULE_DISABLED,
                "El módulo «" + module.label() + "» no está habilitado para esta organización");
    }

    public boolean isRoleAvailable(String roleCode, Set<ClinicModule> modules) {
        ClinicModule required = ROLE_MODULES.get(roleCode);
        return required == null || modules.contains(required);
    }

    public void assertRolesAvailable(Long tenantId, Collection<String> roleCodes) {
        Set<ClinicModule> modules = enabled(tenantId);
        for (String roleCode : roleCodes) {
            if (!isRoleAvailable(roleCode, modules)) {
                throw new BadRequestException(CODE_MODULE_DISABLED,
                        "El rol " + roleCode + " requiere el módulo «" + ROLE_MODULES.get(roleCode).label()
                                + "», que no está habilitado para esta organización");
            }
        }
    }

    public void assertCanSignIn(Long tenantId, Collection<String> roleCodes) {
        if (roleCodes.isEmpty()) {
            return;
        }
        Set<ClinicModule> modules = enabled(tenantId);
        boolean anyAvailable = roleCodes.stream().anyMatch(roleCode -> isRoleAvailable(roleCode, modules));
        if (!anyAvailable) {
            throw new ForbiddenException(CODE_MODULE_DISABLED,
                    "Tu organización no tiene habilitado este acceso. Contacta al administrador");
        }
    }

    @Transactional
    public Set<ClinicModule> replace(Long tenantId, Set<ClinicModule> requested) {
        Set<ClinicModule> target = requested.isEmpty()
                ? EnumSet.noneOf(ClinicModule.class)
                : EnumSet.copyOf(requested);
        assertDependencies(target);
        List<TenantModule> existing = repository.findByTenantId(tenantId);
        Set<ClinicModule> current = toSet(existing);
        repository.deleteAll(existing.stream().filter(row -> !target.contains(row.getModule())).toList());
        repository.saveAll(target.stream()
                .filter(module -> !current.contains(module))
                .map(module -> TenantModule.builder()
                        .tenantId(tenantId)
                        .module(module)
                        .createdBy(TenantContext.getCurrentUserId())
                        .build())
                .toList());
        AfterTransaction.nowAndAfterCompletion(() -> cache.invalidate(tenantId));
        return target;
    }

    private void assertDependencies(Set<ClinicModule> modules) {
        for (ClinicModule module : modules) {
            for (ClinicModule dependency : module.requires()) {
                if (!modules.contains(dependency)) {
                    throw new BadRequestException("MODULE_DEPENDENCY",
                            "El módulo «" + module.label() + "» requiere el módulo «" + dependency.label() + "»");
                }
            }
        }
    }

    private Set<ClinicModule> toSet(List<TenantModule> rows) {
        Set<ClinicModule> modules = EnumSet.noneOf(ClinicModule.class);
        rows.forEach(row -> modules.add(row.getModule()));
        return modules;
    }
}
