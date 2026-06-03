-- =====================================================================
-- staff_branches: asignación de personal operativo (recepción / asistente)
-- a una o más sucursales. Habilita que recepción/asistente solo vean los
-- datos de las sucursales que tienen asignadas.
-- Espejo de la tabla doctor_branches.
-- =====================================================================

CREATE TABLE IF NOT EXISTS staff_branches (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id),
    branch_id   BIGINT       NOT NULL REFERENCES branches(id),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    created_by  BIGINT,
    CONSTRAINT uq_staff_branch UNIQUE (user_id, branch_id)
);

CREATE INDEX IF NOT EXISTS idx_staff_branches_user   ON staff_branches (user_id)   WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_staff_branches_branch ON staff_branches (branch_id) WHERE is_active = TRUE;
