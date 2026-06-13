-- =====================================================================
-- tenant_process_config: procesos/etapas de la cola operativa que cada
-- clínica (tenant) tiene activos. Solo procesos OPCIONALES: TRIAGE,
-- PAYMENT (cobro) y CALLED (llamado). Recepción/check-in y consulta están
-- siempre activos.
--
-- Semántica: la AUSENCIA de fila para un proceso significa que está ACTIVO
-- (default seguro). Por eso no se siembra nada para los tenants existentes:
-- conservan el comportamiento actual sin cambios. Es solo informativo para
-- que el frontend muestre/oculte pantallas; el backend no bloquea el flujo.
-- =====================================================================

CREATE TABLE IF NOT EXISTS tenant_process_config (
    id          BIGSERIAL   PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL REFERENCES tenants(id),
    process     VARCHAR(50) NOT NULL,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_at  TIMESTAMP,
    updated_by  BIGINT,
    CONSTRAINT uq_tenant_process UNIQUE (tenant_id, process)
);

CREATE INDEX IF NOT EXISTS idx_tenant_process_config_tenant ON tenant_process_config (tenant_id);
