package com.medical.medcore.entity.enums;

/**
 * Procesos/etapas de la cola operativa que una clínica puede encender o apagar.
 *
 * Solo se listan los procesos opcionales: recepción/check-in y consulta están
 * siempre activos. Estos flags son meramente informativos para que el frontend
 * muestre/oculte pantallas y métricas; el backend NO bloquea transiciones de
 * {@link AppointmentFlowStatus} en función de esta configuración.
 *
 * Semántica de la configuración: la ausencia de fila en {@code tenant_process_config}
 * para un proceso significa que el proceso está ACTIVO (default seguro).
 */
public enum ClinicProcess {
    TRIAGE,
    PAYMENT,
    CALLED
}
