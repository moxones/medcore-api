package com.medical.medcore.platform.service;

import com.medical.medcore.platform.domain.Tenant;
import com.medical.medcore.platform.dto.OrganizationProfileResponse;
import com.medical.medcore.platform.dto.ThemeOptionsResponse;
import com.medical.medcore.platform.dto.ThemeRequest;
import com.medical.medcore.platform.dto.UpdateOrganizationProfileRequest;
import com.medical.medcore.platform.repository.TenantRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantBrandingService {

    private final TenantRepository tenantRepository;

    public OrganizationProfileResponse profile(Long tenantId) {
        return OrganizationProfileResponse.from(load(tenantId));
    }

    public ThemeOptionsResponse themeOptions() {
        return ThemeOptionsResponse.all();
    }

    @Transactional
    public OrganizationProfileResponse updateProfile(Long tenantId, UpdateOrganizationProfileRequest request) {
        Tenant tenant = load(tenantId);
        tenant.setName(request.name().trim());
        tenant.setSubtitle(blankToNull(request.subtitle()));
        tenant.setContactEmail(blankToNull(request.contactEmail()));
        tenant.setContactPhone(blankToNull(request.contactPhone()));
        tenant.setAddress(blankToNull(request.address()));
        tenant.setTaxId(blankToNull(request.taxId()));
        return OrganizationProfileResponse.from(tenantRepository.save(tenant));
    }

    @Transactional
    public OrganizationProfileResponse updateTheme(Long tenantId, ThemeRequest request) {
        Tenant tenant = load(tenantId);
        applyTheme(tenant, request);
        return OrganizationProfileResponse.from(tenantRepository.save(tenant));
    }

    void applyTheme(Tenant tenant, ThemeRequest request) {
        if (request.preset().custom()) {
            String primary = normalizeColor(request.primaryColor());
            if (primary == null) {
                throw new BadRequestException("THEME_COLOR_REQUIRED",
                        "El tema personalizado necesita un color principal");
            }
            tenant.setPrimaryColor(primary);
            tenant.setAccentColor(normalizeColor(request.accentColor()));
        }
        tenant.setThemePreset(request.preset());
        tenant.setFontFamily(request.fontFamily());
    }

    private Tenant load(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Organización no encontrada"));
    }

    private String normalizeColor(String value) {
        String color = blankToNull(value);
        return color == null ? null : color.toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
