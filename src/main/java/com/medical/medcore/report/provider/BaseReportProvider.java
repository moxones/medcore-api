package com.medical.medcore.report.provider;

import com.medical.medcore.dto.response.report.ReportRange;
import com.medical.medcore.report.ReportContext;
import com.medical.medcore.report.ReportQuery;
import com.medical.medcore.report.support.BranchFilter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Helpers comunes a todos los providers. */
public abstract class BaseReportProvider {

    // --- Conversión de columnas de queries nativas (Number heterogéneos) ---

    protected long asLong(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    protected double asDouble(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0d;
    }

    protected BigDecimal asBig(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        if (o instanceof BigDecimal b) {
            return b;
        }
        return o instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
    }

    protected String asString(Object o) {
        return o != null ? o.toString() : "";
    }

    protected ReportRange rangeOf(ReportQuery q) {
        return new ReportRange(q.from().toString(), q.to().toString());
    }

    protected String generatedAt() {
        return com.medical.medcore.report.support.ReportFormatter.dateTime(LocalDateTime.now());
    }

    /**
     * Alcance por sucursal: el staff con sucursales asignadas queda acotado a ellas (aunque no
     * mande branchId); si además filtra por una sucursal contenida en su alcance, se reduce a esa.
     * Sin alcance asignado, respeta el branchId opcional o devuelve todas (none()).
     */
    protected BranchFilter resolveBranches(ReportQuery q, ReportContext ctx) {
        if (ctx.hasBranchScope()) {
            List<Long> scope = ctx.branchIds();
            if (q.branchId() != null && scope.contains(q.branchId())) {
                return BranchFilter.of(List.of(q.branchId()));
            }
            return BranchFilter.of(scope);
        }
        if (q.branchId() != null) {
            return BranchFilter.of(List.of(q.branchId()));
        }
        return BranchFilter.none();
    }
}
