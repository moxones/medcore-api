package com.medical.medcore.platform.service;

import com.medical.medcore.platform.domain.BusinessType;
import com.medical.medcore.platform.domain.ClinicModule;
import com.medical.medcore.platform.domain.Tenant;
import com.medical.medcore.platform.domain.TenantStatus;
import com.medical.medcore.platform.dto.BusinessTypeResponse;
import com.medical.medcore.platform.dto.CreateTenantRequest;
import com.medical.medcore.platform.dto.TenantInfoResponse;
import com.medical.medcore.platform.dto.TenantResponse;
import com.medical.medcore.platform.dto.UpdateTenantRequest;
import com.medical.medcore.platform.repository.TenantRepository;
import com.medical.medcore.platform.service.TenantDirectory.TenantSnapshot;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.tenancy.TenantContext;
import com.medical.medcore.shared.tenancy.TenantHostResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantService {

    private static final Set<String> RESERVED_SUBDOMAINS = Set.of(
            "www", "api", "admin", "app", "localhost", "default", "mail", "smtp", "imap", "pop", "ftp", "webmail",
            "status", "cdn", "static", "assets", "media", "files", "docs", "help", "soporte", "support", "blog",
            "dev", "test", "staging", "demo", "auth", "login", "sso", "portal", "dashboard", "panel", "billing",
            "pagos", "ns1", "ns2", "dns", "vpn", "git", "monitor", "grafana", "metrics", "root", "system",
            "plataforma", "platform", "medcore");
    private static final String NOT_FOUND = "Organización no encontrada";
    private static final String DEFAULT_TIMEZONE = "America/Lima";
    private static final String DEFAULT_CURRENCY = "PEN";

    private final TenantRepository tenantRepository;
    private final TenantModuleService moduleService;
    private final TenantDirectory directory;
    private final TenantHostResolver hostResolver;
    private final TenantTerminology terminology;
    private final TenantBrandingService branding;

    public TenantInfoResponse getTenantInfo() {
        Tenant tenant = load(TenantContext.requireTenantId());
        boolean expired = directory.find(new TenantHostResolver.TenantLookup(
                        TenantHostResolver.TenantLookup.Kind.SUBDOMAIN, tenant.getSubdomain()))
                .map(TenantSnapshot::subscriptionBlocked)
                .orElse(false);
        return TenantInfoResponse.from(tenant, moduleService.enabled(tenant.getId()), expired);
    }

    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        String subdomain = request.subdomain().trim().toLowerCase(Locale.ROOT);
        if (RESERVED_SUBDOMAINS.contains(subdomain)) {
            throw new BadRequestException("SUBDOMAIN_RESERVED", "El subdominio está reservado");
        }
        if (tenantRepository.existsBySubdomain(subdomain)) {
            throw new ConflictException("SUBDOMAIN_TAKEN", "El subdominio ya existe");
        }
        BusinessType businessType = request.businessType() != null ? request.businessType() : BusinessType.MEDICAL;
        Tenant tenant = Tenant.builder()
                .subdomain(subdomain)
                .customDomain(resolveCustomDomain(request.customDomain(), null))
                .name(request.name().trim())
                .status(TenantStatus.ACTIVE)
                .subtitle(blankToNull(request.subtitle()))
                .contactEmail(blankToNull(request.contactEmail()))
                .contactPhone(blankToNull(request.contactPhone()))
                .address(blankToNull(request.address()))
                .taxId(blankToNull(request.taxId()))
                .timezone(resolveTimezone(request.timezone(), DEFAULT_TIMEZONE))
                .currency(resolveCurrency(request.currency(), DEFAULT_CURRENCY))
                .businessType(businessType)
                .build();
        if (request.theme() != null) {
            branding.applyTheme(tenant, request.theme());
        }
        Tenant saved = tenantRepository.save(tenant);
        Set<ClinicModule> requested = request.modules() != null ? request.modules() : businessType.defaultModules();
        Set<ClinicModule> modules = moduleService.replace(saved.getId(), requested);
        directory.evict(saved.getId());
        return TenantResponse.from(saved, modules);
    }

    @Transactional
    public TenantResponse updateTenant(Long id, UpdateTenantRequest request) {
        Tenant tenant = load(id);
        tenant.setName(request.name().trim());
        tenant.setSubtitle(blankToNull(request.subtitle()));
        if (request.customDomain() != null) {
            tenant.setCustomDomain(resolveCustomDomain(request.customDomain(), tenant.getId()));
        }
        if (request.contactEmail() != null) {
            tenant.setContactEmail(blankToNull(request.contactEmail()));
        }
        if (request.contactPhone() != null) {
            tenant.setContactPhone(blankToNull(request.contactPhone()));
        }
        if (request.address() != null) {
            tenant.setAddress(blankToNull(request.address()));
        }
        if (request.taxId() != null) {
            tenant.setTaxId(blankToNull(request.taxId()));
        }
        tenant.setTimezone(resolveTimezone(request.timezone(), tenant.getTimezone()));
        tenant.setCurrency(resolveCurrency(request.currency(), tenant.getCurrency()));
        if (request.status() != null) {
            changeStatus(tenant, request.status());
        }
        if (request.businessType() != null) {
            tenant.setBusinessType(request.businessType());
        }
        if (request.theme() != null) {
            branding.applyTheme(tenant, request.theme());
        }
        Tenant saved = tenantRepository.save(tenant);
        Set<ClinicModule> modules = request.modules() != null
                ? replaceModules(saved, request.modules())
                : moduleService.enabled(saved.getId());
        directory.evict(saved.getId());
        terminology.evict(saved.getId());
        return TenantResponse.from(saved, modules);
    }

    public TenantResponse getTenantById(Long id) {
        Tenant tenant = load(id);
        return TenantResponse.from(tenant, moduleService.enabled(tenant.getId()));
    }

    public List<TenantResponse> getAllTenants() {
        List<Tenant> tenants = tenantRepository.findAll();
        Map<Long, Set<ClinicModule>> modules = moduleService.enabledByTenant(tenants.stream().map(Tenant::getId).toList());
        return tenants.stream()
                .sorted(Comparator.comparing(Tenant::getId))
                .map(tenant -> TenantResponse.from(tenant, modules.getOrDefault(tenant.getId(), Set.of())))
                .toList();
    }

    public List<BusinessTypeResponse> businessTypes() {
        return Arrays.stream(BusinessType.values()).map(BusinessTypeResponse::from).toList();
    }

    @Transactional
    public void deactivateTenant(Long id) {
        Tenant tenant = load(id);
        changeStatus(tenant, TenantStatus.INACTIVE);
        tenantRepository.save(tenant);
        directory.evict(tenant.getId());
    }

    private Set<ClinicModule> replaceModules(Tenant tenant, Set<ClinicModule> requested) {
        if (directory.isPlatformSubdomain(tenant.getSubdomain()) && !requested.containsAll(ClinicModule.all())) {
            throw new BadRequestException("No se pueden quitar módulos a la organización de la plataforma");
        }
        return moduleService.replace(tenant.getId(), requested);
    }

    private void changeStatus(Tenant tenant, TenantStatus status) {
        if (status == TenantStatus.INACTIVE && directory.isPlatformSubdomain(tenant.getSubdomain())) {
            throw new BadRequestException("No se puede desactivar la organización de la plataforma");
        }
        tenant.setStatus(status);
    }

    private String resolveCustomDomain(String requested, Long tenantId) {
        String domain = blankToNull(requested);
        if (domain == null) {
            return null;
        }
        String normalized = domain.toLowerCase(Locale.ROOT);
        if (hostResolver.isPlatformDomain(normalized)) {
            throw new BadRequestException("CUSTOM_DOMAIN_INVALID",
                    "El dominio propio no puede pertenecer al dominio de la plataforma; usa el subdominio");
        }
        boolean taken = tenantId == null
                ? tenantRepository.findByCustomDomain(normalized).isPresent()
                : tenantRepository.existsByCustomDomainAndIdNot(normalized, tenantId);
        if (taken) {
            throw new ConflictException("CUSTOM_DOMAIN_TAKEN", "El dominio propio ya está asignado a otra organización");
        }
        return normalized;
    }

    private Tenant load(Long id) {
        return tenantRepository.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    private String resolveTimezone(String requested, String fallback) {
        if (requested == null || requested.isBlank()) {
            return fallback;
        }
        try {
            return ZoneId.of(requested.trim()).getId();
        } catch (DateTimeException invalid) {
            throw new BadRequestException("Zona horaria inválida");
        }
    }

    private String resolveCurrency(String requested, String fallback) {
        return requested == null || requested.isBlank() ? fallback : requested.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
