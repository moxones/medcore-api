package com.medical.medcore.identity.service;

import com.medical.medcore.platform.service.TenantDirectory;
import com.medical.medcore.shared.exception.ForbiddenException;
import com.medical.medcore.shared.security.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class PlatformRoleGuard {

    public static final String CODE_PLATFORM_ROLE = "PLATFORM_ROLE_OUTSIDE_PLATFORM";

    private final TenantDirectory directory;

    public void assertAllowed(Long tenantId, Collection<String> roleCodes) {
        if (roleCodes.contains(RoleConstants.SUPER_ADMIN) && !directory.isPlatform(tenantId)) {
            throw new ForbiddenException(CODE_PLATFORM_ROLE,
                    "El rol de super administrador solo existe en la organización de la plataforma");
        }
    }
}
