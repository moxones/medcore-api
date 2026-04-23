package com.medical.medcore.security.authorization.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize(
        "hasAnyAuthority(" +
                "T(com.medical.medcore.security.authorization.constants.RoleConstants).SUPER_ADMIN, " +
                "T(com.medical.medcore.security.authorization.constants.RoleConstants).ADMIN)"
)
public @interface RequireAdminOrSuperAdmin {
}