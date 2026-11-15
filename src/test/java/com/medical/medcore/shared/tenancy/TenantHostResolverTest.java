package com.medical.medcore.shared.tenancy;

import com.medical.medcore.shared.tenancy.TenantHostResolver.TenantLookup;
import com.medical.medcore.shared.tenancy.TenantHostResolver.TenantLookup.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TenantHostResolverTest {

    private final TenantHostResolver production = new TenantHostResolver(List.of("pemedcore.xyz", "localhost"), false);
    private final TenantHostResolver development = new TenantHostResolver(List.of("pemedcore.xyz", "localhost"), true);

    @Test
    void resolvesSubdomainOfBaseDomain() {
        assertThat(production.resolve("Clinica.PeMedCore.xyz", null))
                .contains(new TenantLookup(Kind.SUBDOMAIN, "clinica"));
        assertThat(production.resolve("clinica.localhost", null))
                .contains(new TenantLookup(Kind.SUBDOMAIN, "clinica"));
    }

    @Test
    void rootDomainHasNoTenant() {
        assertThat(production.resolve("pemedcore.xyz", null)).isEmpty();
    }

    @Test
    void nestedSubdomainIsRejected() {
        assertThat(production.resolve("a.clinica.pemedcore.xyz", null)).isEmpty();
    }

    @Test
    void foreignHostIsTreatedAsCustomDomainNotAsSubdomain() {
        assertThat(production.resolve("clinica.otrodominio.com", null))
                .contains(new TenantLookup(Kind.CUSTOM_DOMAIN, "clinica.otrodominio.com"));
    }

    @Test
    void headerIsIgnoredUnlessAllowed() {
        assertThat(production.resolve("localhost", "clinica")).isEmpty();
        assertThat(production.resolve("10.0.0.5", "clinica")).isEmpty();
        assertThat(development.resolve("localhost", "Clinica"))
                .contains(new TenantLookup(Kind.SUBDOMAIN, "clinica"));
    }

    @Test
    void headerNeverOverridesHost() {
        assertThat(development.resolve("clinica.pemedcore.xyz", "otra"))
                .contains(new TenantLookup(Kind.SUBDOMAIN, "clinica"));
    }

    @Test
    void blankHostHasNoTenant() {
        assertThat(production.resolve(" ", "clinica")).isEqualTo(Optional.empty());
    }

    @Test
    void recognisesPlatformDomains() {
        assertThat(production.isPlatformDomain("citas.pemedcore.xyz")).isTrue();
        assertThat(production.isPlatformDomain("pemedcore.xyz")).isTrue();
        assertThat(production.isPlatformDomain("citas.miclinica.com")).isFalse();
    }
}
