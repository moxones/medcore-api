package com.medical.medcore.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Set<String> currentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    public static boolean hasAnyRole(String... roles) {
        Set<String> granted = currentRoles();
        for (String role : roles) {
            if (granted.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSuperAdmin() {
        return hasAnyRole(RoleConstants.SUPER_ADMIN);
    }

    public static boolean isAdmin() {
        return hasAnyRole(RoleConstants.SUPER_ADMIN, RoleConstants.ADMIN);
    }

    public static boolean isDoctor() {
        return hasAnyRole(RoleConstants.DOCTOR);
    }

    public static boolean isPatient() {
        return hasAnyRole(RoleConstants.PATIENT);
    }

    public static boolean isStaff() {
        return hasAnyRole(
                RoleConstants.SUPER_ADMIN,
                RoleConstants.ADMIN,
                RoleConstants.DOCTOR,
                RoleConstants.ASSISTANT,
                RoleConstants.RECEPTIONIST);
    }

    public static boolean isDoctorOnly() {
        return isDoctor() && !isAdmin();
    }

    public static boolean isBranchScoped() {
        if (isAdmin()) {
            return false;
        }
        return hasAnyRole(RoleConstants.RECEPTIONIST, RoleConstants.ASSISTANT);
    }
}
