-- =====================================================================
-- note_templates: plantillas personales de nota clínica de un médico.
-- Permiten precargar los campos de una atención. Cada plantilla pertenece
-- a un médico dentro de un tenant. Pantalla "Plantillas" del portal médico.
-- =====================================================================

CREATE TABLE IF NOT EXISTS note_templates (
    id                   BIGSERIAL    PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL REFERENCES tenants(id),
    doctor_id            BIGINT       NOT NULL REFERENCES doctors(id),
    name                 VARCHAR(150) NOT NULL,
    specialty_name       VARCHAR(150),
    chief_complaint      TEXT,
    present_illness      TEXT,
    physical_examination TEXT,
    assessment           TEXT,
    plan                 TEXT,
    treatment            TEXT,
    notes                TEXT,
    usage_count          BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMP    NOT NULL DEFAULT now(),
    created_by           BIGINT,
    updated_at           TIMESTAMP,
    updated_by           BIGINT
);

CREATE INDEX IF NOT EXISTS idx_note_templates_doctor ON note_templates (tenant_id, doctor_id);
