package com.medical.medcore.shared.tenancy;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class TenantHostResolver {

    private static final String LOCALHOST = "localhost";
    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    private final List<String> baseDomains;
    private final boolean headerAllowed;

    public TenantHostResolver(List<String> baseDomains, boolean headerAllowed) {
        this.baseDomains = baseDomains.stream()
                .map(TenantHostResolver::normalize)
                .filter(domain -> !domain.isEmpty())
                .toList();
        this.headerAllowed = headerAllowed;
    }

    public Optional<TenantLookup> resolve(String host, String tenantHeader) {
        String normalizedHost = normalize(host);
        if (normalizedHost.isEmpty()) {
            return Optional.empty();
        }
        if (isDirectHost(normalizedHost)) {
            return fromHeader(tenantHeader);
        }
        for (String baseDomain : baseDomains) {
            if (normalizedHost.equals(baseDomain)) {
                return Optional.empty();
            }
            String suffix = "." + baseDomain;
            if (normalizedHost.endsWith(suffix)) {
                String label = normalizedHost.substring(0, normalizedHost.length() - suffix.length());
                return label.isEmpty() || label.contains(".")
                        ? Optional.empty()
                        : Optional.of(TenantLookup.subdomain(label));
            }
        }
        return Optional.of(TenantLookup.customDomain(normalizedHost));
    }

    public boolean isPlatformDomain(String domain) {
        String normalized = normalize(domain);
        return baseDomains.stream().anyMatch(base -> normalized.equals(base) || normalized.endsWith("." + base));
    }

    private Optional<TenantLookup> fromHeader(String tenantHeader) {
        if (!headerAllowed || tenantHeader == null || tenantHeader.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(TenantLookup.subdomain(normalize(tenantHeader)));
    }

    private static boolean isDirectHost(String host) {
        return LOCALHOST.equals(host) || IPV4.matcher(host).matches() || host.contains(":");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.endsWith(".") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    public record TenantLookup(Kind kind, String value) {

        public enum Kind { SUBDOMAIN, CUSTOM_DOMAIN }

        static TenantLookup subdomain(String value) {
            return new TenantLookup(Kind.SUBDOMAIN, value);
        }

        static TenantLookup customDomain(String value) {
            return new TenantLookup(Kind.CUSTOM_DOMAIN, value);
        }
    }
}
