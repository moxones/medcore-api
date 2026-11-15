package com.medical.medcore.platform.service;

import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.identity.service.PersonData;
import com.medical.medcore.identity.service.PersonRegistry;
import com.medical.medcore.identity.service.UserAccounts;
import com.medical.medcore.platform.domain.ClinicModule;
import com.medical.medcore.platform.domain.Tenant;
import com.medical.medcore.platform.domain.TenantStatus;
import com.medical.medcore.platform.repository.TenantRepository;
import com.medical.medcore.shared.security.RoleConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import com.medical.medcore.shared.tenancy.TenantContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformBootstrap implements ApplicationRunner {

    private static final String BOOTSTRAP_DOCUMENT_TYPE = "DNI";
    private static final String BOOTSTRAP_DOCUMENT_NUMBER = "00000001";

    private final TenantRepository tenantRepository;
    private final TenantModuleService moduleService;
    private final UserRepository userRepository;
    private final PersonRegistry personRegistry;
    private final UserAccounts userAccounts;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.bootstrap.platform-subdomain:admin}")
    private String platformSubdomain;

    @Value("${app.bootstrap.platform-name:MedCore}")
    private String platformName;

    @Value("${app.bootstrap.super-admin-email:}")
    private String superAdminEmail;

    @Value("${app.bootstrap.super-admin-password:}")
    private String superAdminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (superAdminEmail.isBlank() || superAdminPassword.isBlank()) {
            return;
        }
        TenantContext.runAsSystem(() -> transactionTemplate.executeWithoutResult(status -> bootstrap()));
    }

    private void bootstrap() {
        if (userRepository.existsByRoleCode(RoleConstants.SUPER_ADMIN)) {
            return;
        }
        Tenant platform = tenantRepository.findBySubdomain(platformSubdomain)
                .orElseGet(() -> tenantRepository.save(Tenant.builder()
                        .subdomain(platformSubdomain)
                        .name(platformName)
                        .status(TenantStatus.ACTIVE)
                        .build()));
        moduleService.replace(platform.getId(), ClinicModule.all());
        Person person = personRegistry
                .findByDocument(platform.getId(), BOOTSTRAP_DOCUMENT_TYPE, BOOTSTRAP_DOCUMENT_NUMBER)
                .orElseGet(() -> personRegistry.create(platform.getId(), new PersonData(
                        "Super", "Admin", null, null, null, superAdminEmail, null, null,
                        BOOTSTRAP_DOCUMENT_TYPE, BOOTSTRAP_DOCUMENT_NUMBER)));
        userAccounts.create(platform.getId(), person, superAdminEmail, superAdminPassword,
                Set.of(userAccounts.requireRole(RoleConstants.SUPER_ADMIN)));
        log.info("Super administrador inicial creado en la organización '{}'", platformSubdomain);
    }
}
