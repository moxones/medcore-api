package com.medical.medcore.security.authorization.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize(
        "hasAnyAuthority(" +
                "T(com.medical.medcore.security.authorization.constants.RoleConstants).ADMIN, " +
                "T(com.medical.medcore.security.authorization.constants.RoleConstants).RECEPTIONIST, " +
                "T(com.medical.medcore.security.authorization.constants.RoleConstants).ASSISTANT)"
)
public @interface RequireReception {
}
