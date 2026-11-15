ALTER TABLE public.tenants ADD COLUMN IF NOT EXISTS custom_domain character varying(253);
ALTER TABLE public.tenants ADD CONSTRAINT uq_tenants_custom_domain UNIQUE (custom_domain);

CREATE FUNCTION public.fn_inherit_tenant() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_parent_id bigint;
    v_tenant bigint;
BEGIN
    v_parent_id := (to_jsonb(NEW) ->> TG_ARGV[1])::bigint;
    EXECUTE format('SELECT tenant_id FROM public.%I WHERE id = $1', TG_ARGV[0])
       INTO v_tenant
      USING v_parent_id;
    IF v_tenant IS NULL THEN
        RAISE EXCEPTION 'No se encontró el registro padre %.% = % para %', TG_ARGV[0], 'id', v_parent_id, TG_TABLE_NAME
              USING ERRCODE = 'foreign_key_violation';
    END IF;
    NEW.tenant_id := v_tenant;
    RETURN NEW;
END;
$$;

DO $$
DECLARE
    v_child record;
BEGIN
    FOR v_child IN
        SELECT * FROM (VALUES
            ('appointment_flow_history', 'appointments', 'appointment_id'),
            ('appointment_reschedules', 'appointments', 'appointment_id'),
            ('doctor_branches', 'doctors', 'doctor_id'),
            ('doctor_schedules', 'doctors', 'doctor_id'),
            ('doctor_specialties', 'doctors', 'doctor_id'),
            ('staff_branches', 'users', 'user_id'),
            ('user_roles', 'users', 'user_id'),
            ('patient_allergies', 'patients', 'patient_id'),
            ('patient_conditions', 'patients', 'patient_id'),
            ('patient_family_history', 'patients', 'patient_id'),
            ('patient_habits', 'patients', 'patient_id'),
            ('patient_surgical_history', 'patients', 'patient_id'),
            ('medical_certificates', 'medical_entries', 'medical_entry_id'),
            ('medical_entry_diagnoses', 'medical_entries', 'medical_entry_id'),
            ('medical_files', 'medical_entries', 'medical_entry_id'),
            ('medical_orders', 'medical_entries', 'medical_entry_id'),
            ('medical_procedures', 'medical_entries', 'medical_entry_id'),
            ('prescriptions', 'medical_entries', 'medical_entry_id'),
            ('medical_order_results', 'medical_orders', 'medical_order_id')
        ) AS t(child, parent, fk)
    LOOP
        EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS tenant_id bigint', v_child.child);
        EXECUTE format('UPDATE public.%I c SET tenant_id = p.tenant_id FROM public.%I p WHERE p.id = c.%I AND c.tenant_id IS NULL',
                       v_child.child, v_child.parent, v_child.fk);
        EXECUTE format('ALTER TABLE public.%I ALTER COLUMN tenant_id SET NOT NULL', v_child.child);
        EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT %I FOREIGN KEY (tenant_id) REFERENCES public.tenants (id)',
                       v_child.child, 'fk_' || v_child.child || '_tenant');
        EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON public.%I (tenant_id)',
                       'idx_' || v_child.child || '_tenant', v_child.child);
        EXECUTE format('CREATE TRIGGER %I BEFORE INSERT OR UPDATE OF %I ON public.%I FOR EACH ROW EXECUTE FUNCTION public.fn_inherit_tenant(%L, %L)',
                       'trg_' || v_child.child || '_tenant', v_child.fk, v_child.child, v_child.parent, v_child.fk);
    END LOOP;
END;
$$;

CREATE FUNCTION public.app_tenant_visible(row_tenant bigint) RETURNS boolean
    LANGUAGE sql STABLE
    AS $$
    SELECT COALESCE(current_setting('app.rls_bypass', true), '') = 'on'
        OR row_tenant = NULLIF(current_setting('app.current_tenant_id', true), '')::bigint
$$;

DO $$
DECLARE
    v_table text;
BEGIN
    FOREACH v_table IN ARRAY ARRAY[
        'tenant_modules', 'tenant_process_config', 'tenant_specialties', 'tenant_appointment_types',
        'tenant_document_types', 'subscriptions', 'persons', 'person_documents', 'branches', 'users', 'user_roles',
        'staff_branches', 'refresh_tokens', 'password_reset_tokens', 'doctors', 'doctor_specialties',
        'doctor_branches', 'doctor_schedules', 'note_templates', 'patients', 'patient_allergies',
        'patient_conditions', 'patient_family_history', 'patient_habits', 'patient_surgical_history',
        'appointments', 'appointment_flow_history', 'appointment_reschedules', 'payments', 'triage',
        'medical_records', 'medical_entries', 'medical_entry_diagnoses', 'medical_procedures', 'medical_files',
        'medical_certificates', 'prescriptions', 'medical_orders', 'medical_order_results', 'audit_log']
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', v_table);
        EXECUTE format('CREATE POLICY tenant_isolation ON public.%I USING (public.app_tenant_visible(tenant_id)) WITH CHECK (public.app_tenant_visible(tenant_id))',
                       v_table);
    END LOOP;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'medcore_app') THEN
        CREATE ROLE medcore_app NOLOGIN;
    END IF;
END;
$$;

GRANT USAGE ON SCHEMA public TO medcore_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO medcore_app;
REVOKE ALL ON public.flyway_schema_history FROM medcore_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO medcore_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO medcore_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO medcore_app;
