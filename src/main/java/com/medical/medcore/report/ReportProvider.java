package com.medical.medcore.report;

import com.medical.medcore.dto.response.report.ReportResult;

import java.util.Set;

/**
 * Un proveedor por cada reporte. Spring inyecta todos los {@code @Component} que implementan
 * esta interfaz y {@link ReportService} los registra por {@link #key()}.
 *
 * <p>Para agregar un reporte nuevo basta con crear una clase: no se toca el controller ni el
 * servicio.
 */
public interface ReportProvider {

    /** Clave estable del reporte (la que envía el frontend), p. ej. "clinic-financial-summary". */
    String key();

    /** Roles autorizados a consumir este reporte (constantes de RoleConstants). */
    Set<String> allowedRoles();

    /**
     * Genera el sobre estándar. El rango en {@code query} ya viene normalizado y validado,
     * y {@code ctx} trae tenant/usuario/roles/sucursales resueltos en servidor.
     */
    ReportResult generate(ReportQuery query, ReportContext ctx);
}
