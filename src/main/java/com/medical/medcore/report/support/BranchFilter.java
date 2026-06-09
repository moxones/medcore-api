package com.medical.medcore.report.support;

import java.util.List;

/**
 * Resultado de resolver el alcance por sucursal de un reporte. Cuando {@code apply} es false,
 * {@code branchIds} contiene un valor centinela inofensivo para que el {@code IN (:branchIds)}
 * nativo no reciba una lista vacía.
 */
public record BranchFilter(boolean apply, List<Long> branchIds) {

    private static final BranchFilter NONE = new BranchFilter(false, List.of(-1L));

    public static BranchFilter none() {
        return NONE;
    }

    public static BranchFilter of(List<Long> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) {
            return NONE;
        }
        return new BranchFilter(true, branchIds);
    }
}
