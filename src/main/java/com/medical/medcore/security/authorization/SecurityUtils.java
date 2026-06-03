package com.medical.medcore.security.authorization;

import com.medical.medcore.security.authorization.constants.RoleConstants;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Set<String> currentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Set.of();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    public static boolean hasAnyRole(String... roles) {
        Set<String> granted = currentRoles();
        for (String r : roles) {
            if (granted.contains(r)) return true;
        }
        return false;
    }

    public static boolean isStaff() {
        return hasAnyRole(
                RoleConstants.SUPER_ADMIN,
                RoleConstants.ADMIN,
                RoleConstants.DOCTOR,
                RoleConstants.ASSISTANT,
                RoleConstants.RECEPTIONIST);
    }

    public static boolean isPatient() {
        return hasAnyRole(RoleConstants.PATIENT);
    }

    /**
     * Indica si el usuario autenticado debe ver solo las sucursales que tiene asignadas.
     * Admins y super-admins ven todo el tenant; el personal operativo (recepción / asistente)
     * queda restringido a sus sucursales.
     */
    public static boolean isBranchScoped() {
        if (hasAnyRole(RoleConstants.SUPER_ADMIN, RoleConstants.ADMIN)) {
            return false;
        }
        return hasAnyRole(RoleConstants.RECEPTIONIST, RoleConstants.ASSISTANT);
    }
}
