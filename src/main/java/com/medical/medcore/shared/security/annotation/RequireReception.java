package com.medical.medcore.shared.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize(
        "hasAnyAuthority(" +
                "T(com.medical.medcore.shared.security.RoleConstants).SUPER_ADMIN, " +
                "T(com.medical.medcore.shared.security.RoleConstants).ADMIN, " +
                "T(com.medical.medcore.shared.security.RoleConstants).RECEPTIONIST, " +
                "T(com.medical.medcore.shared.security.RoleConstants).ASSISTANT)"
)
public @interface RequireReception {
}
