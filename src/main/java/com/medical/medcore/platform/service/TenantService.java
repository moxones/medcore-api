package com.medical.medcore.platform.service;

import com.medical.medcore.platform.domain.Tenant;
import com.medical.medcore.platform.domain.TenantStatus;
import com.medical.medcore.platform.dto.CreateTenantRequest;
import com.medical.medcore.platform.dto.TenantInfoResponse;
import com.medical.medcore.platform.dto.TenantResponse;
import com.medical.medcore.platform.dto.UpdateTenantRequest;
import com.medical.medcore.platform.repository.TenantRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantService {

    private static final Set<String> RESERVED_SUBDOMAINS = Set.of("www", "api", "admin", "app", "localhost", "default");
    private static final String NOT_FOUND = "Organización no encontrada";
    private static final String DEFAULT_TIMEZONE = "America/Lima";
    private static final String DEFAULT_CURRENCY = "PEN";

    private final TenantRepository tenantRepository;

    @Value("${app.bootstrap.platform-subdomain:healthcare}")
    private String platformSubdomain;

    public TenantInfoResponse getTenantInfo() {
        return TenantInfoResponse.from(load(TenantContext.requireTenantId()));
    }

    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        String subdomain = request.subdomain().trim().toLowerCase();
        if (RESERVED_SUBDOMAINS.contains(subdomain)) {
            throw new BadRequestException("El subdominio está reservado");
        }
        if (tenantRepository.existsBySubdomain(subdomain)) {
            throw new ConflictException("SUBDOMAIN_TAKEN", "El subdominio ya existe");
        }
        Tenant tenant = Tenant.builder()
                .subdomain(subdomain)
                .name(request.name().trim())
                .status(TenantStatus.ACTIVE)
                .logoUrl(blankToNull(request.logoUrl()))
                .primaryColor(blankToNull(request.primaryColor()))
                .subtitle(blankToNull(request.subtitle()))
                .contactEmail(blankToNull(request.contactEmail()))
                .contactPhone(blankToNull(request.contactPhone()))
                .address(blankToNull(request.address()))
                .taxId(blankToNull(request.taxId()))
                .timezone(resolveTimezone(request.timezone(), DEFAULT_TIMEZONE))
                .currency(resolveCurrency(request.currency(), DEFAULT_CURRENCY))
                .build();
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    @Transactional
    public TenantResponse updateTenant(Long id, UpdateTenantRequest request) {
        Tenant tenant = load(id);
        tenant.setName(request.name().trim());
        tenant.setLogoUrl(blankToNull(request.logoUrl()));
        tenant.setPrimaryColor(blankToNull(request.primaryColor()));
        tenant.setSubtitle(blankToNull(request.subtitle()));
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
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    public TenantResponse getTenantById(Long id) {
        return TenantResponse.from(load(id));
    }

    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll().stream()
                .sorted(Comparator.comparing(Tenant::getId))
                .map(TenantResponse::from)
                .toList();
    }

    @Transactional
    public void deactivateTenant(Long id) {
        Tenant tenant = load(id);
        changeStatus(tenant, TenantStatus.INACTIVE);
        tenantRepository.save(tenant);
    }

    private void changeStatus(Tenant tenant, TenantStatus status) {
        if (status == TenantStatus.INACTIVE && tenant.getSubdomain().equals(platformSubdomain)) {
            throw new BadRequestException("No se puede desactivar la organización de la plataforma");
        }
        tenant.setStatus(status);
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
