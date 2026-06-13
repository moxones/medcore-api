package com.medical.medcore.service.tenant;

import com.medical.medcore.entity.enums.ClinicProcess;

import java.util.Map;

public interface TenantProcessConfigService {

    /**
     * Configuración efectiva de procesos de la clínica actual. Incluye SIEMPRE
     * todos los procesos: los que no tienen fila se devuelven como activos.
     */
    Map<ClinicProcess, Boolean> getConfig();

    /**
     * Activa/desactiva los procesos indicados (upsert) para la clínica actual.
     * Devuelve la configuración efectiva resultante.
     */
    Map<ClinicProcess, Boolean> updateConfig(Map<ClinicProcess, Boolean> processes);
}
