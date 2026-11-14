ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS last_login_at timestamp without time zone,
    ADD COLUMN IF NOT EXISTS failed_login_attempts integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until timestamp without time zone,
    ADD COLUMN IF NOT EXISTS password_changed_at timestamp without time zone,
    ADD COLUMN IF NOT EXISTS must_change_password boolean NOT NULL DEFAULT false;

ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_branch_id_fkey;
ALTER TABLE public.users DROP COLUMN IF EXISTS branch_id;
ALTER TABLE public.users DROP CONSTRAINT IF EXISTS unique_tenant_email;
UPDATE public.users SET email = lower(email) WHERE email <> lower(email);
UPDATE public.users SET is_active = true WHERE is_active IS NULL;
ALTER TABLE public.users ALTER COLUMN is_active SET NOT NULL;

CREATE TABLE IF NOT EXISTS public.password_reset_tokens (
    id bigserial PRIMARY KEY,
    tenant_id bigint NOT NULL REFERENCES public.tenants (id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
    token_hash character varying(64) NOT NULL UNIQUE,
    expires_at timestamp without time zone NOT NULL,
    used_at timestamp without time zone,
    requested_ip character varying(64),
    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user ON public.password_reset_tokens (user_id);

ALTER TABLE public.persons
    ADD COLUMN IF NOT EXISTS address character varying(255),
    ADD COLUMN IF NOT EXISTS district character varying(100);

ALTER TABLE public.person_documents ADD COLUMN IF NOT EXISTS tenant_id bigint;
UPDATE public.person_documents pd
   SET tenant_id = p.tenant_id
  FROM public.persons p
 WHERE p.id = pd.person_id AND pd.tenant_id IS NULL;
ALTER TABLE public.person_documents ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.person_documents
    ADD CONSTRAINT fk_person_documents_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants (id);
ALTER TABLE public.person_documents DROP CONSTRAINT IF EXISTS unique_document;
ALTER TABLE public.person_documents
    ADD CONSTRAINT uq_person_document_per_tenant UNIQUE (tenant_id, document_type_id, document_number);

ALTER TABLE public.patients
    ADD COLUMN IF NOT EXISTS emergency_contact_name character varying(150),
    ADD COLUMN IF NOT EXISTS emergency_contact_phone character varying(50);
UPDATE public.patients SET is_active = true WHERE is_active IS NULL;
UPDATE public.patients
   SET medical_record_number = 'HC-' || lpad(id::text, 6, '0')
 WHERE medical_record_number IS NULL;
ALTER TABLE public.patients
    ADD CONSTRAINT uq_patient_record_number UNIQUE (tenant_id, medical_record_number);

ALTER TABLE public.appointments
    ADD COLUMN IF NOT EXISTS cancel_reason text,
    ADD COLUMN IF NOT EXISTS cancelled_at timestamp without time zone,
    ADD COLUMN IF NOT EXISTS cancelled_by bigint,
    ADD COLUMN IF NOT EXISTS no_show_at timestamp without time zone,
    ADD COLUMN IF NOT EXISTS price numeric(10, 2);
ALTER TABLE public.appointments ALTER COLUMN flow_status SET DEFAULT 'SCHEDULED';

UPDATE public.appointments a
   SET status_id = (SELECT id FROM public.appointment_status WHERE code = 'CANCELLED'),
       cancel_reason = COALESCE(a.cancel_reason, a.reason),
       cancelled_at = COALESCE(a.cancelled_at, a.updated_at, a.created_at)
 WHERE a.status_id = (SELECT id FROM public.appointment_status WHERE code = 'NO_SHOW');

UPDATE public.appointments a
   SET status_id = (SELECT id FROM public.appointment_status WHERE code = 'COMPLETED')
 WHERE a.flow_status = 'COMPLETED'
   AND a.status_id = (SELECT id FROM public.appointment_status WHERE code = 'SCHEDULED');

UPDATE public.appointments a
   SET branch_id = (SELECT db.branch_id FROM public.doctor_branches db WHERE db.doctor_id = a.doctor_id ORDER BY db.id LIMIT 1)
 WHERE a.branch_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_appointments_tenant_status ON public.appointments (tenant_id, status_id);

ALTER TABLE public.appointment_flow_history
    ADD COLUMN IF NOT EXISTS from_status character varying(50),
    ADD COLUMN IF NOT EXISTS changed_by bigint,
    ADD COLUMN IF NOT EXISTS note character varying(255);
ALTER TABLE public.appointment_flow_history DROP CONSTRAINT IF EXISTS appointment_flow_history_appointment_id_fkey;
ALTER TABLE public.appointment_flow_history DROP CONSTRAINT IF EXISTS fk_afh_appointment;
ALTER TABLE public.appointment_flow_history
    ADD CONSTRAINT fk_afh_appointment FOREIGN KEY (appointment_id) REFERENCES public.appointments (id) ON DELETE CASCADE;

ALTER TABLE public.tenant_appointment_types ADD COLUMN IF NOT EXISTS price numeric(10, 2);
ALTER TABLE public.tenant_appointment_types
    ADD CONSTRAINT chk_tenant_appt_type_price CHECK (price IS NULL OR price >= 0);

ALTER TABLE public.payments
    ADD COLUMN IF NOT EXISTS tenant_id bigint,
    ADD COLUMN IF NOT EXISTS reference character varying(100),
    ADD COLUMN IF NOT EXISTS voided_at timestamp without time zone,
    ADD COLUMN IF NOT EXISTS voided_by bigint,
    ADD COLUMN IF NOT EXISTS void_reason character varying(255),
    ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone,
    ADD COLUMN IF NOT EXISTS updated_by bigint;
UPDATE public.payments p
   SET tenant_id = a.tenant_id
  FROM public.appointments a
 WHERE a.id = p.appointment_id AND p.tenant_id IS NULL;
ALTER TABLE public.payments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.payments
    ADD CONSTRAINT fk_payments_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants (id);
UPDATE public.payments SET status = 'COMPLETED' WHERE status NOT IN ('PENDING', 'COMPLETED', 'VOIDED');
ALTER TABLE public.payments
    ADD CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'COMPLETED', 'VOIDED')),
    ADD CONSTRAINT chk_payments_amount CHECK (amount > 0);
CREATE INDEX IF NOT EXISTS idx_payments_tenant_date ON public.payments (tenant_id, payment_date);

ALTER TABLE public.triage ADD COLUMN IF NOT EXISTS tenant_id bigint;
UPDATE public.triage t
   SET tenant_id = a.tenant_id
  FROM public.appointments a
 WHERE a.id = t.appointment_id AND t.tenant_id IS NULL;
ALTER TABLE public.triage ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.triage
    ADD CONSTRAINT fk_triage_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants (id);
CREATE INDEX IF NOT EXISTS idx_triage_tenant ON public.triage (tenant_id);

ALTER TABLE public.medical_records
    ADD COLUMN IF NOT EXISTS tenant_id bigint;
UPDATE public.medical_records r
   SET tenant_id = p.tenant_id
  FROM public.patients p
 WHERE p.id = r.patient_id AND r.tenant_id IS NULL;
ALTER TABLE public.medical_records ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.medical_records
    ADD CONSTRAINT fk_medical_records_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants (id);

ALTER TABLE public.medical_entries
    ADD COLUMN IF NOT EXISTS tenant_id bigint,
    ADD COLUMN IF NOT EXISTS doctor_id bigint;
UPDATE public.medical_entries e
   SET tenant_id = r.tenant_id
  FROM public.medical_records r
 WHERE r.id = e.medical_record_id AND e.tenant_id IS NULL;
UPDATE public.medical_entries e
   SET doctor_id = a.doctor_id
  FROM public.appointments a
 WHERE a.id = e.appointment_id AND e.doctor_id IS NULL;
ALTER TABLE public.medical_entries ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE public.medical_entries
    ADD CONSTRAINT fk_medical_entries_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants (id),
    ADD CONSTRAINT fk_medical_entries_doctor FOREIGN KEY (doctor_id) REFERENCES public.doctors (id);
CREATE INDEX IF NOT EXISTS idx_medical_entries_tenant ON public.medical_entries (tenant_id);
CREATE INDEX IF NOT EXISTS idx_medical_entries_doctor ON public.medical_entries (doctor_id);

ALTER TABLE public.tenants
    ADD COLUMN IF NOT EXISTS contact_email character varying(150),
    ADD COLUMN IF NOT EXISTS contact_phone character varying(50),
    ADD COLUMN IF NOT EXISTS address character varying(255),
    ADD COLUMN IF NOT EXISTS tax_id character varying(20),
    ADD COLUMN IF NOT EXISTS timezone character varying(60) NOT NULL DEFAULT 'America/Lima',
    ADD COLUMN IF NOT EXISTS currency character varying(3) NOT NULL DEFAULT 'PEN';

ALTER TABLE public.branches
    ADD COLUMN IF NOT EXISTS phone character varying(50),
    ADD COLUMN IF NOT EXISTS created_by bigint,
    ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone,
    ADD COLUMN IF NOT EXISTS updated_by bigint;
UPDATE public.branches SET is_active = true WHERE is_active IS NULL;
ALTER TABLE public.branches ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE public.subscriptions
    ADD COLUMN IF NOT EXISTS created_by bigint,
    ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone,
    ADD COLUMN IF NOT EXISTS updated_by bigint;

ALTER TABLE public.subscription_status
    ADD COLUMN IF NOT EXISTS name character varying(100),
    ADD COLUMN IF NOT EXISTS is_active boolean NOT NULL DEFAULT true;

INSERT INTO public.subscription_status (code, name)
SELECT v.code, v.name
  FROM (VALUES ('TRIAL', 'Prueba'), ('ACTIVE', 'Activa'), ('SUSPENDED', 'Suspendida'),
               ('CANCELLED', 'Cancelada'), ('EXPIRED', 'Vencida')) AS v(code, name)
 WHERE NOT EXISTS (SELECT 1 FROM public.subscription_status s WHERE s.code = v.code);

UPDATE public.payments
   SET payment_method = 'OTHER'
 WHERE payment_method IS NOT NULL
   AND payment_method NOT IN ('CASH', 'CARD', 'TRANSFER', 'DIGITAL_WALLET', 'INSURANCE', 'OTHER');
