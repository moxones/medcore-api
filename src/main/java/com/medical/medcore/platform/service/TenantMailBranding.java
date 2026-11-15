package com.medical.medcore.platform.service;

import com.medical.medcore.platform.domain.Tenant;
import com.medical.medcore.platform.domain.TenantLogo;
import com.medical.medcore.platform.domain.ThemePreset;
import com.medical.medcore.platform.repository.TenantLogoRepository;
import com.medical.medcore.platform.repository.TenantRepository;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.notification.MailBranding;
import com.medical.medcore.shared.tenancy.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Arma la marca con la que sale cada correo. Lee como sistema porque el super admin también crea cuentas (e invita)
 * en organizaciones que no son la suya. Los enlaces apuntan al subdominio de la organización (o a su dominio propio)
 * según {@code app.mail.link-base-url}, que admite el marcador {@code {subdomain}}.
 */
@Service
public class TenantMailBranding {

    private static final Set<String> EMAIL_SAFE_LOGOS = Set.of("image/png", "image/jpeg");
    private static final String SUBDOMAIN_PLACEHOLDER = "{subdomain}";

    private final TenantRepository tenantRepository;
    private final TenantLogoRepository logoRepository;
    private final String linkBaseUrl;

    public TenantMailBranding(TenantRepository tenantRepository, TenantLogoRepository logoRepository,
                              @Value("${app.mail.link-base-url:http://{subdomain}.localhost:4200}") String linkBaseUrl) {
        this.tenantRepository = tenantRepository;
        this.logoRepository = logoRepository;
        this.linkBaseUrl = linkBaseUrl;
    }

    public MailBranding forTenant(Long tenantId) {
        return TenantContext.runAsSystem(() -> {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new NotFoundException("Organización no encontrada"));
            TenantLogo logo = logoRepository.findById(tenantId)
                    .filter(candidate -> EMAIL_SAFE_LOGOS.contains(candidate.getContentType()))
                    .orElse(null);
            return new MailBranding(
                    tenant.getName(),
                    primaryColor(tenant),
                    logo != null ? logo.getContent() : null,
                    logo != null ? logo.getContentType() : null,
                    baseUrl(tenant),
                    blankToNull(tenant.getContactEmail()));
        });
    }

    private String baseUrl(Tenant tenant) {
        if (tenant.getCustomDomain() != null && !tenant.getCustomDomain().isBlank()) {
            return "https://" + tenant.getCustomDomain().trim();
        }
        return linkBaseUrl.replace(SUBDOMAIN_PLACEHOLDER, tenant.getSubdomain());
    }

    private static String primaryColor(Tenant tenant) {
        ThemePreset preset = tenant.getThemePreset();
        if (preset == null || preset.custom()) {
            return tenant.getPrimaryColor();
        }
        return preset.primaryColor();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
