package com.medical.medcore.report;

import java.util.List;
import java.util.Set;

/**
 * Contexto de seguridad ya resuelto en servidor para generar un reporte.
 * El tenant, el usuario, los roles y las sucursales nunca vienen del cliente.
 */
public record ReportContext(
        Long tenantId,
        Long userId,
        Set<String> roles,
        List<Long> branchIds
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /** true si el usuario tiene sucursales acotadas (staff con scope por sucursal). */
    public boolean hasBranchScope() {
        return branchIds != null && !branchIds.isEmpty();
    }
}
