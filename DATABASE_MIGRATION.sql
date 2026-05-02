-- ============================================
-- MEDCORE API - DATABASE MIGRATION SCRIPT
-- Execute this script to fix all issues found in backend review
-- ============================================

BEGIN;

-- ============================================
-- 1. INDEXES FOR PERFORMANCE
-- ============================================

-- person_documents: FK to persons (used in frequent JOINs)
CREATE INDEX IF NOT EXISTS idx_person_documents_person
ON public.person_documents(person_id);

-- appointments: index for searches by doctor and date
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_scheduled
ON public.appointments(doctor_id, scheduled_at);

-- appointments: index for flow_status searches
CREATE INDEX IF NOT EXISTS idx_appointments_flow_status
ON public.appointments(flow_status);

-- users: index for tenant searches
CREATE INDEX IF NOT EXISTS idx_users_tenant_id
ON public.users(tenant_id);

-- persons: index for tenant searches
CREATE INDEX IF NOT EXISTS idx_persons_tenant_id
ON public.persons(tenant_id);

-- patients: index for tenant searches
CREATE INDEX IF NOT EXISTS idx_patients_tenant_id
ON public.patients(tenant_id);

-- doctors: index for tenant searches
CREATE INDEX IF NOT EXISTS idx_doctors_tenant_id
ON public.doctors(tenant_id);

-- branches: index for tenant searches
CREATE INDEX IF NOT EXISTS idx_branches_tenant_id
ON public.branches(tenant_id);

-- ============================================
-- 2. UNIQUE CONSTRAINTS VERIFICATION
-- ============================================

-- doctors: ensure unique per tenant (if not exists)
DO $$
BEGIN
   IF NOT EXISTS (
      SELECT 1 FROM pg_constraint WHERE conname = 'unique_doctor_per_tenant'
   ) THEN
      ALTER TABLE public.doctors
      ADD CONSTRAINT unique_doctor_per_tenant UNIQUE (tenant_id, person_id);
   END IF;
END $$;

-- patients: ensure unique per tenant (if not exists)
DO $$
BEGIN
   IF NOT EXISTS (
      SELECT 1 FROM pg_constraint WHERE conname = 'unique_patient_per_tenant'
   ) THEN
      ALTER TABLE public.patients
      ADD CONSTRAINT unique_patient_per_tenant UNIQUE (tenant_id, person_id);
   END IF;
END $$;

-- medical_records: ensure unique per patient (if not exists)
DO $$
BEGIN
   IF NOT EXISTS (
      SELECT 1 FROM pg_constraint WHERE conname = 'unique_record_per_patient'
   ) THEN
      ALTER TABLE public.medical_records
      ADD CONSTRAINT unique_record_per_patient UNIQUE (patient_id);
   END IF;
END $$;

-- doctor_specialties: ensure unique per doctor-specialty (if not exists)
DO $$
BEGIN
   IF NOT EXISTS (
      SELECT 1 FROM pg_constraint WHERE conname = 'unique_doctor_specialty'
   ) THEN
      ALTER TABLE public.doctor_specialties
      ADD CONSTRAINT unique_doctor_specialty UNIQUE (doctor_id, specialty_id);
   END IF;
END $$;

-- ============================================
-- 3. TRIGGER FOR DOCUMENT UNIQUE PER TENANT (Opción B)
-- Validation: document_number unique by document_type per TENANT
-- ============================================

-- Function to check document uniqueness per tenant
CREATE OR REPLACE FUNCTION check_document_uniqueness()
RETURNS TRIGGER AS $$
DECLARE
    v_person_tenant_id bigint;
BEGIN
    -- Get tenant_id from the person associated with this document
    SELECT tenant_id INTO v_person_tenant_id
    FROM persons
    WHERE id = NEW.person_id;

    -- Check if another document with same type and number exists for same tenant
    IF EXISTS (
        SELECT 1 FROM person_documents pd
        JOIN persons p ON pd.person_id = p.id
        WHERE pd.document_type_id = NEW.document_type_id
          AND pd.document_number = NEW.document_number
          AND p.tenant_id = v_person_tenant_id
          AND pd.person_id != NEW.person_id
    ) THEN
        RAISE EXCEPTION 'Documento ya existe para este tenant';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop trigger if exists and recreate
DROP TRIGGER IF EXISTS trg_check_document_uniqueness ON person_documents;

CREATE TRIGGER trg_check_document_uniqueness
BEFORE INSERT OR UPDATE ON person_documents
FOR EACH ROW
EXECUTE FUNCTION check_document_uniqueness();

-- ============================================
-- 4. ON DELETE CASCADE CONSTRAINTS
-- ============================================

-- appointments: tenant cascade
ALTER TABLE public.appointments
DROP CONSTRAINT IF EXISTS fk_appointments_tenant;
ALTER TABLE public.appointments
ADD CONSTRAINT fk_appointments_tenant
FOREIGN KEY (tenant_id) REFERENCES public.tenants(id)
ON DELETE CASCADE;

-- refresh_tokens: user cascade
ALTER TABLE public.refresh_tokens
DROP CONSTRAINT IF EXISTS fk_refresh_tokens_user;
ALTER TABLE public.refresh_tokens
ADD CONSTRAINT fk_refresh_tokens_user
FOREIGN KEY (user_id) REFERENCES public.users(id)
ON DELETE CASCADE;

-- user_roles: user cascade
ALTER TABLE public.user_roles
DROP CONSTRAINT IF EXISTS fk_user_roles_user;
ALTER TABLE public.user_roles
ADD CONSTRAINT fk_user_roles_user
FOREIGN KEY (user_id) REFERENCES public.users(id)
ON DELETE CASCADE;

-- doctor_schedules: doctor cascade
ALTER TABLE public.doctor_schedules
DROP CONSTRAINT IF EXISTS fk_ds_doctor;
ALTER TABLE public.doctor_schedules
ADD CONSTRAINT fk_ds_doctor
FOREIGN KEY (doctor_id) REFERENCES public.doctors(id)
ON DELETE CASCADE;

-- doctor_schedules: branch cascade
ALTER TABLE public.doctor_schedules
DROP CONSTRAINT IF EXISTS fk_ds_branch;
ALTER TABLE public.doctor_schedules
ADD CONSTRAINT fk_ds_branch
FOREIGN KEY (branch_id) REFERENCES public.branches(id)
ON DELETE CASCADE;

-- triage: appointment cascade
ALTER TABLE public.triage
DROP CONSTRAINT IF EXISTS fk_triage_appointment;
ALTER TABLE public.triage
ADD CONSTRAINT fk_triage_appointment
FOREIGN KEY (appointment_id) REFERENCES public.appointments(id)
ON DELETE CASCADE;

-- ============================================
-- 5. APPOINTMENT_RESCHEDULES FK
-- ============================================

ALTER TABLE public.appointment_reschedules
DROP CONSTRAINT IF EXISTS fk_reschedules_appointment;
ALTER TABLE public.appointment_reschedules
ADD CONSTRAINT fk_reschedules_appointment
FOREIGN KEY (appointment_id) REFERENCES public.appointments(id)
ON DELETE CASCADE;

-- ============================================
-- 6. APPOINTMENTS: FK TO APPOINTMENT_STATUS
-- ============================================

ALTER TABLE public.appointments
DROP CONSTRAINT IF EXISTS fk_appointments_status;
ALTER TABLE public.appointments
ADD CONSTRAINT fk_appointments_status
FOREIGN KEY (status_id) REFERENCES public.appointment_status(id)
ON DELETE RESTRICT;

-- ============================================
-- 7. DATA MASTERS - Seed minimal required data
-- ============================================

-- Appointment statuses
INSERT INTO public.appointment_status (code, name)
VALUES
    ('SCHEDULED', 'Programada'),
    ('CONFIRMED', 'Confirmada'),
    ('WAITING', 'En espera'),
    ('IN_PROGRESS', 'En consulta'),
    ('COMPLETED', 'Completada'),
    ('CANCELLED', 'Cancelada'),
    ('NO_SHOW', 'No asistió')
ON CONFLICT (code) DO NOTHING;

-- Roles
INSERT INTO public.roles (code, name, is_active, created_at)
VALUES
    ('SUPER_ADMIN', 'Super Administrador', true, NOW()),
    ('ADMIN', 'Administrador', true, NOW()),
    ('DOCTOR', 'Doctor', true, NOW()),
    ('ASSISTANT', 'Asistente', true, NOW()),
    ('PATIENT', 'Paciente', true, NOW())
ON CONFLICT (code) DO NOTHING;

-- Document types
INSERT INTO public.document_types (code, name, is_active, created_at)
VALUES
    ('DNI', 'Documento Nacional de Identidad', true, NOW()),
    ('PASSPORT', 'Pasaporte', true, NOW()),
    ('CC', 'Cédula de Ciudadanía', true, NOW()),
    ('CE', 'Cédula de Extranjería', true, NOW())
ON CONFLICT (code) DO NOTHING;

-- Plans
INSERT INTO public.plans (code, name, price, max_users, max_branches, is_active, created_at)
VALUES
    ('FREE', 'Plan Gratuito', 0, 5, 1, true, NOW()),
    ('BASIC', 'Plan Básico', 99.99, 20, 3, true, NOW()),
    ('PRO', 'Plan Profesional', 299.99, 100, 10, true, NOW()),
    ('ENTERPRISE', 'Plan Empresarial', 999.99, -1, -1, true, NOW())
ON CONFLICT (code) DO NOTHING;

-- Subscription statuses
INSERT INTO public.subscription_status (code)
VALUES
    ('ACTIVE'),
    ('EXPIRED'),
    ('CANCELLED'),
    ('PENDING')
ON CONFLICT (code) DO NOTHING;

-- ============================================
-- 8. SPECIALTIES SEED (if none exist)
-- ============================================

INSERT INTO public.specialties (name, code, is_active, created_at, tenant_id)
SELECT 'Medicina General', 'GENERAL', true, NOW(), t.id
FROM public.tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM public.specialties s WHERE s.code = 'GENERAL' AND s.tenant_id = t.id
);

INSERT INTO public.specialties (name, code, is_active, created_at, tenant_id)
SELECT 'Pediatría', 'PEDIATRICS', true, NOW(), t.id
FROM public.tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM public.specialties s WHERE s.code = 'PEDIATRICS' AND s.tenant_id = t.id
);

INSERT INTO public.specialties (name, code, is_active, created_at, tenant_id)
SELECT 'Cardiología', 'CARDIOLOGY', true, NOW(), t.id
FROM public.tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM public.specialties s WHERE s.code = 'CARDIOLOGY' AND s.tenant_id = t.id
);

-- ============================================
-- 9. APPOINTMENT_TYPES SEED (for existing tenants)
-- ============================================

INSERT INTO public.appointment_types (name, code, duration_minutes, is_active, tenant_id)
SELECT 'Consulta General', 'GENERAL_CONSULTATION', 30, true, t.id
FROM public.tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM public.appointment_types at
    WHERE at.code = 'GENERAL_CONSULTATION' AND at.tenant_id = t.id
);

INSERT INTO public.appointment_types (name, code, duration_minutes, is_active, tenant_id)
SELECT 'Control', 'FOLLOW_UP', 15, true, t.id
FROM public.tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM public.appointment_types at
    WHERE at.code = 'FOLLOW_UP' AND at.tenant_id = t.id
);

INSERT INTO public.appointment_types (name, code, duration_minutes, is_active, tenant_id)
SELECT 'Emergencia', 'EMERGENCY', 60, true, t.id
FROM public.tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM public.appointment_types at
    WHERE at.code = 'EMERGENCY' AND at.tenant_id = t.id
);

COMMIT;

-- ============================================
-- VERIFICATION QUERIES (run these to confirm)
-- ============================================

-- Check indexes created
-- SELECT indexname, tablename FROM pg_indexes WHERE schemaname = 'public' ORDER BY tablename, indexname;

-- Check constraints
-- SELECT conname, conrelid::regclass FROM pg_constraint WHERE conrelid::regclass::text ~ 'public\.' ORDER BY conrelid;

-- Check trigger exists
-- SELECT tgname, tgrelid::regclass FROM pg_trigger WHERE tgname = 'trg_check_document_uniqueness';

-- Check data masters
-- SELECT 'roles' as table_name, count(*) as count FROM roles
-- UNION ALL SELECT 'appointment_status', count(*) FROM appointment_status
-- UNION ALL SELECT 'document_types', count(*) FROM document_types
-- UNION ALL SELECT 'plans', count(*) FROM plans
-- UNION ALL SELECT 'subscription_status', count(*) FROM subscription_status;