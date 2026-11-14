CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;

CREATE FUNCTION public.fn_audit_clinical() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_actor bigint;
    v_tenant bigint;
BEGIN
    BEGIN
        v_actor := NULLIF(current_setting('app.current_user_id', true), '')::bigint;
        v_tenant := NULLIF(current_setting('app.current_tenant_id', true), '')::bigint;
    EXCEPTION WHEN others THEN
        v_actor := NULL;
        v_tenant := NULL;
    END;

    IF (TG_OP = 'DELETE') THEN
        INSERT INTO public.audit_log(tenant_id, table_name, record_id, action, old_values, changed_by)
        VALUES (v_tenant, TG_TABLE_NAME, OLD.id, 'DELETE', to_jsonb(OLD), v_actor);
        RETURN OLD;
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO public.audit_log(tenant_id, table_name, record_id, action, old_values, new_values, changed_by)
        VALUES (v_tenant, TG_TABLE_NAME, NEW.id, 'UPDATE', to_jsonb(OLD), to_jsonb(NEW), v_actor);
        RETURN NEW;
    ELSE
        INSERT INTO public.audit_log(tenant_id, table_name, record_id, action, new_values, changed_by)
        VALUES (v_tenant, TG_TABLE_NAME, NEW.id, 'INSERT', to_jsonb(NEW), v_actor);
        RETURN NEW;
    END IF;
END;
$$;

CREATE FUNCTION public.fn_block_locked_entry() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF (OLD.is_locked = true) THEN
        RAISE EXCEPTION 'La nota clínica % está firmada y no puede modificarse', OLD.id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TABLE public.appointment_flow_history (
    id bigint NOT NULL,
    appointment_id bigint NOT NULL,
    flow_status character varying(50) NOT NULL,
    changed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE public.appointment_flow_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.appointment_flow_history_id_seq OWNED BY public.appointment_flow_history.id;

CREATE TABLE public.appointment_reschedules (
    id bigint NOT NULL,
    appointment_id bigint NOT NULL,
    old_scheduled_at timestamp without time zone NOT NULL,
    new_scheduled_at timestamp without time zone NOT NULL,
    reason text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint
);

CREATE SEQUENCE public.appointment_reschedules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.appointment_reschedules_id_seq OWNED BY public.appointment_reschedules.id;

CREATE TABLE public.appointment_status (
    id bigint NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by bigint,
    updated_by bigint,
    is_active boolean DEFAULT true
);

CREATE SEQUENCE public.appointment_status_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.appointment_status_id_seq OWNED BY public.appointment_status.id;

CREATE TABLE public.appointment_types (
    id bigint NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    duration_minutes integer DEFAULT 30 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by bigint,
    updated_by bigint
);

CREATE SEQUENCE public.appointment_types_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.appointment_types_id_seq OWNED BY public.appointment_types.id;

CREATE TABLE public.appointments (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    patient_id bigint NOT NULL,
    doctor_id bigint NOT NULL,
    scheduled_at timestamp without time zone NOT NULL,
    status_id bigint NOT NULL,
    reason text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp without time zone,
    updated_by bigint,
    duration_minutes integer DEFAULT 30 NOT NULL,
    branch_id bigint,
    appointment_type_id bigint,
    flow_status character varying(50) DEFAULT 'WAITING'::character varying NOT NULL,
    booking_source character varying(30),
    checked_in_at timestamp without time zone,
    called_at timestamp without time zone,
    started_at timestamp without time zone,
    finished_at timestamp without time zone,
    completed_at timestamp without time zone,
    CONSTRAINT chk_appointments_flow_status CHECK (((flow_status)::text = ANY ((ARRAY['SCHEDULED'::character varying, 'WAITING'::character varying, 'CALLED'::character varying, 'IN_PROCESS'::character varying, 'PENDING_PAYMENT'::character varying, 'COMPLETED'::character varying])::text[]))),
    CONSTRAINT chk_duration_positive CHECK ((duration_minutes > 0))
);

CREATE SEQUENCE public.appointments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.appointments_id_seq OWNED BY public.appointments.id;

CREATE TABLE public.audit_log (
    id bigint NOT NULL,
    tenant_id bigint,
    table_name character varying(100) NOT NULL,
    record_id bigint NOT NULL,
    action character varying(10) NOT NULL,
    old_values jsonb,
    new_values jsonb,
    changed_by bigint,
    changed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ip_address character varying(45)
);

CREATE SEQUENCE public.audit_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.audit_log_id_seq OWNED BY public.audit_log.id;

CREATE TABLE public.branches (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    name character varying(150) NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    appointment_duration_minutes integer DEFAULT 30,
    address character varying(255) DEFAULT ''::character varying NOT NULL,
    ruc character varying(20),
    opening_time time without time zone,
    closing_time time without time zone
);

COMMENT ON COLUMN public.branches.address IS 'Dirección obligatoria de la sucursal';

COMMENT ON COLUMN public.branches.ruc IS 'RUC de la clínica (opcional)';

COMMENT ON COLUMN public.branches.opening_time IS 'Horario de apertura';

COMMENT ON COLUMN public.branches.closing_time IS 'Horario de cierre';

CREATE SEQUENCE public.branches_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.branches_id_seq OWNED BY public.branches.id;

CREATE TABLE public.cie10_codes (
    id bigint NOT NULL,
    code character varying(10) NOT NULL,
    description character varying(300) NOT NULL,
    category character varying(150),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE public.cie10_codes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.cie10_codes_id_seq OWNED BY public.cie10_codes.id;

CREATE TABLE public.doctor_branches (
    id bigint NOT NULL,
    doctor_id bigint NOT NULL,
    branch_id bigint NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

CREATE SEQUENCE public.doctor_branches_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.doctor_branches_id_seq OWNED BY public.doctor_branches.id;

CREATE TABLE public.doctor_schedules (
    id bigint NOT NULL,
    doctor_id bigint NOT NULL,
    branch_id bigint,
    day_of_week integer NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    is_active boolean DEFAULT true,
    doctor_branch_id bigint,
    slot_duration_minutes integer DEFAULT 30 NOT NULL,
    max_patients_per_slot integer,
    valid_from date,
    valid_until date,
    created_at timestamp without time zone DEFAULT now(),
    created_by bigint,
    updated_at timestamp without time zone,
    updated_by bigint,
    CONSTRAINT chk_day_of_week CHECK (((day_of_week >= 0) AND (day_of_week <= 6))),
    CONSTRAINT chk_ds_time CHECK ((end_time > start_time))
);

COMMENT ON COLUMN public.doctor_schedules.day_of_week IS '0=Lunes, 1=Martes, 2=Miércoles, 3=Jueves, 4=Viernes, 5=Sábado, 6=Domingo';

CREATE SEQUENCE public.doctor_schedules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.doctor_schedules_id_seq OWNED BY public.doctor_schedules.id;

CREATE TABLE public.doctor_specialties (
    id bigint NOT NULL,
    doctor_id bigint NOT NULL,
    specialty_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE public.doctor_specialties_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.doctor_specialties_id_seq OWNED BY public.doctor_specialties.id;

CREATE TABLE public.doctors (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    person_id bigint NOT NULL,
    license_number character varying(50),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp without time zone,
    updated_by bigint
);

CREATE SEQUENCE public.doctors_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.doctors_id_seq OWNED BY public.doctors.id;

CREATE TABLE public.document_types (
    id bigint NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by bigint,
    updated_by bigint
);

CREATE SEQUENCE public.document_types_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.document_types_id_seq OWNED BY public.document_types.id;

CREATE TABLE public.medical_certificates (
    id bigint NOT NULL,
    medical_entry_id bigint NOT NULL,
    certificate_type character varying(30) NOT NULL,
    content text NOT NULL,
    rest_days integer,
    issued_at timestamp without time zone DEFAULT now() NOT NULL,
    valid_until date,
    created_by bigint
);

COMMENT ON COLUMN public.medical_certificates.certificate_type IS 'MEDICAL_REST | FITNESS | ATTENDANCE | OTHER';

CREATE SEQUENCE public.medical_certificates_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.medical_certificates_id_seq OWNED BY public.medical_certificates.id;

CREATE TABLE public.medical_entries (
    id bigint NOT NULL,
    medical_record_id bigint NOT NULL,
    appointment_id bigint,
    diagnosis text,
    treatment text,
    notes text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    entry_type character varying(30) DEFAULT 'CONSULTATION'::character varying NOT NULL,
    chief_complaint text,
    present_illness text,
    review_of_systems text,
    physical_examination text,
    assessment text,
    plan text,
    follow_up_at date,
    updated_at timestamp without time zone,
    updated_by bigint,
    signed_by bigint,
    signed_at timestamp without time zone,
    is_locked boolean DEFAULT false NOT NULL
);

COMMENT ON COLUMN public.medical_entries.entry_type IS 'CONSULTATION | CONTROL | EMERGENCY | PROCEDURE';

COMMENT ON COLUMN public.medical_entries.is_locked IS 'true = nota firmada/cerrada, no editable';

CREATE SEQUENCE public.medical_entries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.medical_entries_id_seq OWNED BY public.medical_entries.id;

CREATE TABLE public.medical_entry_diagnoses (
    id bigint NOT NULL,
    medical_entry_id bigint NOT NULL,
    cie10_id bigint,
    description character varying(300) NOT NULL,
    diagnosis_type character varying(20) DEFAULT 'DEFINITIVE'::character varying NOT NULL,
    diagnosis_rank character varying(20) DEFAULT 'PRIMARY'::character varying NOT NULL,
    notes text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

COMMENT ON COLUMN public.medical_entry_diagnoses.diagnosis_type IS 'PRESUMPTIVE | DEFINITIVE';

COMMENT ON COLUMN public.medical_entry_diagnoses.diagnosis_rank IS 'PRIMARY | SECONDARY';

CREATE SEQUENCE public.medical_entry_diagnoses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.medical_entry_diagnoses_id_seq OWNED BY public.medical_entry_diagnoses.id;

CREATE TABLE public.medical_files (
    id bigint NOT NULL,
    medical_entry_id bigint NOT NULL,
    file_url text NOT NULL,
    file_type character varying(50),
    uploaded_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE public.medical_files_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.medical_files_id_seq OWNED BY public.medical_files.id;

CREATE TABLE public.medical_order_results (
    id bigint NOT NULL,
    medical_order_id bigint NOT NULL,
    result text,
    file_url text,
    result_date timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

CREATE SEQUENCE public.medical_order_results_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.medical_order_results_id_seq OWNED BY public.medical_order_results.id;

CREATE TABLE public.medical_orders (
    id bigint NOT NULL,
    medical_entry_id bigint NOT NULL,
    order_type character varying(30) NOT NULL,
    description text NOT NULL,
    status character varying(20) DEFAULT 'REQUESTED'::character varying NOT NULL,
    requested_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

COMMENT ON COLUMN public.medical_orders.order_type IS 'LAB | IMAGING | PROCEDURE | REFERRAL | OTHER';

COMMENT ON COLUMN public.medical_orders.status IS 'REQUESTED | IN_PROGRESS | COMPLETED | CANCELLED';

CREATE SEQUENCE public.medical_orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.medical_orders_id_seq OWNED BY public.medical_orders.id;

CREATE TABLE public.medical_procedures (
    id bigint NOT NULL,
    medical_entry_id bigint NOT NULL,
    code character varying(50),
    name character varying(255) NOT NULL,
    notes text,
    performed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

CREATE SEQUENCE public.medical_procedures_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.medical_procedures_id_seq OWNED BY public.medical_procedures.id;

CREATE TABLE public.medical_records (
    id bigint NOT NULL,
    patient_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint
);

CREATE SEQUENCE public.medical_records_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.medical_records_id_seq OWNED BY public.medical_records.id;

CREATE TABLE public.note_templates (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    doctor_id bigint NOT NULL,
    name character varying(150) NOT NULL,
    specialty_name character varying(150),
    chief_complaint text,
    present_illness text,
    physical_examination text,
    assessment text,
    plan text,
    treatment text,
    notes text,
    usage_count bigint DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp without time zone,
    updated_by bigint
);

CREATE SEQUENCE public.note_templates_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.note_templates_id_seq OWNED BY public.note_templates.id;

CREATE TABLE public.patient_allergies (
    id bigint NOT NULL,
    patient_id bigint NOT NULL,
    allergen character varying(200) NOT NULL,
    reaction character varying(255),
    severity character varying(20),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

COMMENT ON COLUMN public.patient_allergies.severity IS 'MILD | MODERATE | SEVERE';

CREATE SEQUENCE public.patient_allergies_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.patient_allergies_id_seq OWNED BY public.patient_allergies.id;

CREATE TABLE public.patient_conditions (
    id bigint NOT NULL,
    patient_id bigint NOT NULL,
    cie10_id bigint,
    description character varying(255) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    diagnosed_at date,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

COMMENT ON COLUMN public.patient_conditions.status IS 'ACTIVE | RESOLVED | CHRONIC';

CREATE SEQUENCE public.patient_conditions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.patient_conditions_id_seq OWNED BY public.patient_conditions.id;

CREATE TABLE public.patient_family_history (
    id bigint NOT NULL,
    patient_id bigint NOT NULL,
    relationship character varying(50) NOT NULL,
    condition character varying(255) NOT NULL,
    notes text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

CREATE SEQUENCE public.patient_family_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.patient_family_history_id_seq OWNED BY public.patient_family_history.id;

CREATE TABLE public.patient_habits (
    id bigint NOT NULL,
    patient_id bigint NOT NULL,
    habit_type character varying(30) NOT NULL,
    detail character varying(255),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

COMMENT ON COLUMN public.patient_habits.habit_type IS 'TOBACCO | ALCOHOL | DRUGS | PHYSICAL_ACTIVITY | DIET';

CREATE SEQUENCE public.patient_habits_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.patient_habits_id_seq OWNED BY public.patient_habits.id;

CREATE TABLE public.patient_surgical_history (
    id bigint NOT NULL,
    patient_id bigint NOT NULL,
    procedure character varying(255) NOT NULL,
    performed_on date,
    notes text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

CREATE SEQUENCE public.patient_surgical_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.patient_surgical_history_id_seq OWNED BY public.patient_surgical_history.id;

CREATE TABLE public.patients (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    person_id bigint NOT NULL,
    medical_record_number character varying(50),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp without time zone,
    updated_by bigint,
    blood_type character varying(5),
    allergies text,
    chronic_conditions text,
    clinical_notes text
);

CREATE SEQUENCE public.patients_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.patients_id_seq OWNED BY public.patients.id;

CREATE TABLE public.payments (
    id bigint NOT NULL,
    appointment_id bigint NOT NULL,
    amount numeric(10,2) NOT NULL,
    status character varying(50) NOT NULL,
    payment_date timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    payment_method character varying(30),
    concept character varying(200),
    created_by bigint
);

CREATE SEQUENCE public.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.payments_id_seq OWNED BY public.payments.id;

CREATE TABLE public.person_documents (
    id bigint NOT NULL,
    person_id bigint NOT NULL,
    document_type_id bigint NOT NULL,
    document_number character varying(50) NOT NULL,
    is_primary boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE public.person_documents_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.person_documents_id_seq OWNED BY public.person_documents.id;

CREATE TABLE public.persons (
    id bigint NOT NULL,
    first_name character varying(100),
    last_name character varying(100),
    birth_date date,
    gender character varying(20),
    phone character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp without time zone,
    updated_by bigint,
    tenant_id bigint NOT NULL,
    profile_completed boolean DEFAULT false NOT NULL,
    contact_email character varying(150)
);

CREATE SEQUENCE public.persons_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.persons_id_seq OWNED BY public.persons.id;

CREATE TABLE public.plans (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    code character varying(50) NOT NULL,
    price numeric(10,2) NOT NULL,
    max_users integer,
    max_branches integer,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    created_by bigint,
    updated_by bigint
);

CREATE SEQUENCE public.plans_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.plans_id_seq OWNED BY public.plans.id;

CREATE TABLE public.prescriptions (
    id bigint NOT NULL,
    medical_entry_id bigint NOT NULL,
    medication character varying(200) NOT NULL,
    dosage character varying(100),
    frequency character varying(100),
    duration character varying(100),
    instructions text,
    created_at timestamp without time zone DEFAULT now(),
    route character varying(50),
    quantity character varying(50),
    presentation character varying(100),
    is_active boolean DEFAULT true NOT NULL
);

COMMENT ON COLUMN public.prescriptions.route IS 'Vía: ORAL | IV | IM | SC | TOPICAL | OTHER';

CREATE SEQUENCE public.prescriptions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.prescriptions_id_seq OWNED BY public.prescriptions.id;

CREATE TABLE public.refresh_tokens (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    token character varying NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    is_revoked boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_refresh_tokens_dates CHECK ((expires_at > created_at))
);

CREATE SEQUENCE public.refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.refresh_tokens_id_seq OWNED BY public.refresh_tokens.id;

CREATE TABLE public.roles (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    code character varying(50) NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp without time zone,
    updated_by bigint
);

CREATE SEQUENCE public.roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.roles_id_seq OWNED BY public.roles.id;

CREATE TABLE public.specialties (
    id bigint NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    description character varying(255),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by bigint,
    updated_by bigint
);

CREATE SEQUENCE public.specialties_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.specialties_id_seq OWNED BY public.specialties.id;

CREATE TABLE public.staff_branches (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    branch_id bigint NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

CREATE SEQUENCE public.staff_branches_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.staff_branches_id_seq OWNED BY public.staff_branches.id;

CREATE TABLE public.subscription_status (
    id bigint NOT NULL,
    code character varying(50),
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by bigint,
    updated_by bigint
);

CREATE SEQUENCE public.subscription_status_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.subscription_status_id_seq OWNED BY public.subscription_status.id;

CREATE TABLE public.subscriptions (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    plan_id bigint NOT NULL,
    start_date date NOT NULL,
    end_date date,
    status character varying(50) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE public.subscriptions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.subscriptions_id_seq OWNED BY public.subscriptions.id;

CREATE TABLE public.tenant_appointment_types (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    appointment_type_id bigint NOT NULL,
    duration_minutes integer,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

CREATE SEQUENCE public.tenant_appointment_types_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.tenant_appointment_types_id_seq OWNED BY public.tenant_appointment_types.id;

CREATE TABLE public.tenant_document_types (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    document_type_id bigint NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

CREATE SEQUENCE public.tenant_document_types_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.tenant_document_types_id_seq OWNED BY public.tenant_document_types.id;

CREATE TABLE public.tenant_process_config (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    process character varying(50) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp without time zone,
    updated_by bigint
);

CREATE SEQUENCE public.tenant_process_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.tenant_process_config_id_seq OWNED BY public.tenant_process_config.id;

CREATE TABLE public.tenant_specialties (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    specialty_id bigint NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

CREATE SEQUENCE public.tenant_specialties_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.tenant_specialties_id_seq OWNED BY public.tenant_specialties.id;

CREATE TABLE public.tenants (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    subdomain character varying(100) NOT NULL,
    logo_url text,
    primary_color character varying(20),
    secondary_color character varying(20),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp without time zone,
    updated_by bigint,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    subtitle character varying(150),
    CONSTRAINT chk_tenants_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

CREATE SEQUENCE public.tenants_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.tenants_id_seq OWNED BY public.tenants.id;

CREATE TABLE public.triage (
    id bigint NOT NULL,
    appointment_id bigint NOT NULL,
    weight numeric(5,2),
    height numeric(5,2),
    temperature numeric(4,2),
    heart_rate integer,
    blood_pressure character varying(20),
    notes text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    respiratory_rate integer,
    oxygen_saturation numeric(5,2),
    systolic_pressure integer,
    diastolic_pressure integer,
    pain_scale smallint,
    blood_glucose numeric(5,2),
    bmi numeric(5,2),
    priority_level character varying(20),
    priority_system character varying(20) DEFAULT 'ESI'::character varying,
    measured_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    updated_by bigint,
    CONSTRAINT chk_triage_pain_scale CHECK (((pain_scale IS NULL) OR ((pain_scale >= 0) AND (pain_scale <= 10)))),
    CONSTRAINT chk_triage_spo2 CHECK (((oxygen_saturation IS NULL) OR ((oxygen_saturation >= (0)::numeric) AND (oxygen_saturation <= (100)::numeric))))
);

COMMENT ON COLUMN public.triage.priority_level IS 'Nivel asignado: 1-5 (ESI) o color (Manchester)';

COMMENT ON COLUMN public.triage.priority_system IS 'ESI | MANCHESTER';

CREATE SEQUENCE public.triage_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.triage_id_seq OWNED BY public.triage.id;

CREATE TABLE public.user_roles (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint
);

CREATE SEQUENCE public.user_roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.user_roles_id_seq OWNED BY public.user_roles.id;

CREATE TABLE public.users (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    email character varying(150) NOT NULL,
    password character varying(255) NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp without time zone,
    updated_by bigint,
    person_id bigint,
    branch_id bigint
);

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;

ALTER TABLE ONLY public.appointment_flow_history ALTER COLUMN id SET DEFAULT nextval('public.appointment_flow_history_id_seq'::regclass);

ALTER TABLE ONLY public.appointment_reschedules ALTER COLUMN id SET DEFAULT nextval('public.appointment_reschedules_id_seq'::regclass);

ALTER TABLE ONLY public.appointment_status ALTER COLUMN id SET DEFAULT nextval('public.appointment_status_id_seq'::regclass);

ALTER TABLE ONLY public.appointment_types ALTER COLUMN id SET DEFAULT nextval('public.appointment_types_id_seq'::regclass);

ALTER TABLE ONLY public.appointments ALTER COLUMN id SET DEFAULT nextval('public.appointments_id_seq'::regclass);

ALTER TABLE ONLY public.audit_log ALTER COLUMN id SET DEFAULT nextval('public.audit_log_id_seq'::regclass);

ALTER TABLE ONLY public.branches ALTER COLUMN id SET DEFAULT nextval('public.branches_id_seq'::regclass);

ALTER TABLE ONLY public.cie10_codes ALTER COLUMN id SET DEFAULT nextval('public.cie10_codes_id_seq'::regclass);

ALTER TABLE ONLY public.doctor_branches ALTER COLUMN id SET DEFAULT nextval('public.doctor_branches_id_seq'::regclass);

ALTER TABLE ONLY public.doctor_schedules ALTER COLUMN id SET DEFAULT nextval('public.doctor_schedules_id_seq'::regclass);

ALTER TABLE ONLY public.doctor_specialties ALTER COLUMN id SET DEFAULT nextval('public.doctor_specialties_id_seq'::regclass);

ALTER TABLE ONLY public.doctors ALTER COLUMN id SET DEFAULT nextval('public.doctors_id_seq'::regclass);

ALTER TABLE ONLY public.document_types ALTER COLUMN id SET DEFAULT nextval('public.document_types_id_seq'::regclass);

ALTER TABLE ONLY public.medical_certificates ALTER COLUMN id SET DEFAULT nextval('public.medical_certificates_id_seq'::regclass);

ALTER TABLE ONLY public.medical_entries ALTER COLUMN id SET DEFAULT nextval('public.medical_entries_id_seq'::regclass);

ALTER TABLE ONLY public.medical_entry_diagnoses ALTER COLUMN id SET DEFAULT nextval('public.medical_entry_diagnoses_id_seq'::regclass);

ALTER TABLE ONLY public.medical_files ALTER COLUMN id SET DEFAULT nextval('public.medical_files_id_seq'::regclass);

ALTER TABLE ONLY public.medical_order_results ALTER COLUMN id SET DEFAULT nextval('public.medical_order_results_id_seq'::regclass);

ALTER TABLE ONLY public.medical_orders ALTER COLUMN id SET DEFAULT nextval('public.medical_orders_id_seq'::regclass);

ALTER TABLE ONLY public.medical_procedures ALTER COLUMN id SET DEFAULT nextval('public.medical_procedures_id_seq'::regclass);

ALTER TABLE ONLY public.medical_records ALTER COLUMN id SET DEFAULT nextval('public.medical_records_id_seq'::regclass);

ALTER TABLE ONLY public.note_templates ALTER COLUMN id SET DEFAULT nextval('public.note_templates_id_seq'::regclass);

ALTER TABLE ONLY public.patient_allergies ALTER COLUMN id SET DEFAULT nextval('public.patient_allergies_id_seq'::regclass);

ALTER TABLE ONLY public.patient_conditions ALTER COLUMN id SET DEFAULT nextval('public.patient_conditions_id_seq'::regclass);

ALTER TABLE ONLY public.patient_family_history ALTER COLUMN id SET DEFAULT nextval('public.patient_family_history_id_seq'::regclass);

ALTER TABLE ONLY public.patient_habits ALTER COLUMN id SET DEFAULT nextval('public.patient_habits_id_seq'::regclass);

ALTER TABLE ONLY public.patient_surgical_history ALTER COLUMN id SET DEFAULT nextval('public.patient_surgical_history_id_seq'::regclass);

ALTER TABLE ONLY public.patients ALTER COLUMN id SET DEFAULT nextval('public.patients_id_seq'::regclass);

ALTER TABLE ONLY public.payments ALTER COLUMN id SET DEFAULT nextval('public.payments_id_seq'::regclass);

ALTER TABLE ONLY public.person_documents ALTER COLUMN id SET DEFAULT nextval('public.person_documents_id_seq'::regclass);

ALTER TABLE ONLY public.persons ALTER COLUMN id SET DEFAULT nextval('public.persons_id_seq'::regclass);

ALTER TABLE ONLY public.plans ALTER COLUMN id SET DEFAULT nextval('public.plans_id_seq'::regclass);

ALTER TABLE ONLY public.prescriptions ALTER COLUMN id SET DEFAULT nextval('public.prescriptions_id_seq'::regclass);

ALTER TABLE ONLY public.refresh_tokens ALTER COLUMN id SET DEFAULT nextval('public.refresh_tokens_id_seq'::regclass);

ALTER TABLE ONLY public.roles ALTER COLUMN id SET DEFAULT nextval('public.roles_id_seq'::regclass);

ALTER TABLE ONLY public.specialties ALTER COLUMN id SET DEFAULT nextval('public.specialties_id_seq'::regclass);

ALTER TABLE ONLY public.staff_branches ALTER COLUMN id SET DEFAULT nextval('public.staff_branches_id_seq'::regclass);

ALTER TABLE ONLY public.subscription_status ALTER COLUMN id SET DEFAULT nextval('public.subscription_status_id_seq'::regclass);

ALTER TABLE ONLY public.subscriptions ALTER COLUMN id SET DEFAULT nextval('public.subscriptions_id_seq'::regclass);

ALTER TABLE ONLY public.tenant_appointment_types ALTER COLUMN id SET DEFAULT nextval('public.tenant_appointment_types_id_seq'::regclass);

ALTER TABLE ONLY public.tenant_document_types ALTER COLUMN id SET DEFAULT nextval('public.tenant_document_types_id_seq'::regclass);

ALTER TABLE ONLY public.tenant_process_config ALTER COLUMN id SET DEFAULT nextval('public.tenant_process_config_id_seq'::regclass);

ALTER TABLE ONLY public.tenant_specialties ALTER COLUMN id SET DEFAULT nextval('public.tenant_specialties_id_seq'::regclass);

ALTER TABLE ONLY public.tenants ALTER COLUMN id SET DEFAULT nextval('public.tenants_id_seq'::regclass);

ALTER TABLE ONLY public.triage ALTER COLUMN id SET DEFAULT nextval('public.triage_id_seq'::regclass);

ALTER TABLE ONLY public.user_roles ALTER COLUMN id SET DEFAULT nextval('public.user_roles_id_seq'::regclass);

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);

ALTER TABLE ONLY public.appointment_flow_history
    ADD CONSTRAINT appointment_flow_history_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.appointment_reschedules
    ADD CONSTRAINT appointment_reschedules_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.appointment_status
    ADD CONSTRAINT appointment_status_code_key UNIQUE (code);

ALTER TABLE ONLY public.appointment_status
    ADD CONSTRAINT appointment_status_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.appointment_types
    ADD CONSTRAINT appointment_types_code_key UNIQUE (code);

ALTER TABLE ONLY public.appointment_types
    ADD CONSTRAINT appointment_types_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT appointments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.branches
    ADD CONSTRAINT branches_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.cie10_codes
    ADD CONSTRAINT cie10_codes_code_key UNIQUE (code);

ALTER TABLE ONLY public.cie10_codes
    ADD CONSTRAINT cie10_codes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.doctor_branches
    ADD CONSTRAINT doctor_branches_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.doctor_schedules
    ADD CONSTRAINT doctor_schedules_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.doctor_specialties
    ADD CONSTRAINT doctor_specialties_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.doctors
    ADD CONSTRAINT doctors_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.document_types
    ADD CONSTRAINT document_types_code_key UNIQUE (code);

ALTER TABLE ONLY public.document_types
    ADD CONSTRAINT document_types_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.medical_certificates
    ADD CONSTRAINT medical_certificates_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.medical_entries
    ADD CONSTRAINT medical_entries_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.medical_entry_diagnoses
    ADD CONSTRAINT medical_entry_diagnoses_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.medical_files
    ADD CONSTRAINT medical_files_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.medical_order_results
    ADD CONSTRAINT medical_order_results_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.medical_orders
    ADD CONSTRAINT medical_orders_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.medical_procedures
    ADD CONSTRAINT medical_procedures_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.medical_records
    ADD CONSTRAINT medical_records_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT no_overlap_appointments EXCLUDE USING gist (doctor_id WITH =, tsrange(scheduled_at, (scheduled_at + make_interval(mins => duration_minutes))) WITH &&) WHERE ((status_id = ANY (ARRAY[(1)::bigint, (5)::bigint])));

ALTER TABLE ONLY public.note_templates
    ADD CONSTRAINT note_templates_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.patient_allergies
    ADD CONSTRAINT patient_allergies_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.patient_conditions
    ADD CONSTRAINT patient_conditions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.patient_family_history
    ADD CONSTRAINT patient_family_history_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.patient_habits
    ADD CONSTRAINT patient_habits_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.patient_surgical_history
    ADD CONSTRAINT patient_surgical_history_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.patients
    ADD CONSTRAINT patients_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.person_documents
    ADD CONSTRAINT person_documents_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.persons
    ADD CONSTRAINT persons_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.plans
    ADD CONSTRAINT plans_code_key UNIQUE (code);

ALTER TABLE ONLY public.plans
    ADD CONSTRAINT plans_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.prescriptions
    ADD CONSTRAINT prescriptions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_key UNIQUE (token);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_code_key UNIQUE (code);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.specialties
    ADD CONSTRAINT specialties_code_key UNIQUE (code);

ALTER TABLE ONLY public.specialties
    ADD CONSTRAINT specialties_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.staff_branches
    ADD CONSTRAINT staff_branches_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.subscription_status
    ADD CONSTRAINT subscription_status_code_key UNIQUE (code);

ALTER TABLE ONLY public.subscription_status
    ADD CONSTRAINT subscription_status_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT subscriptions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tenant_appointment_types
    ADD CONSTRAINT tenant_appointment_types_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tenant_document_types
    ADD CONSTRAINT tenant_document_types_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tenant_process_config
    ADD CONSTRAINT tenant_process_config_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tenant_specialties
    ADD CONSTRAINT tenant_specialties_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_subdomain_key UNIQUE (subdomain);

ALTER TABLE ONLY public.triage
    ADD CONSTRAINT triage_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.doctors
    ADD CONSTRAINT unique_doctor_per_tenant UNIQUE (tenant_id, person_id);

ALTER TABLE ONLY public.person_documents
    ADD CONSTRAINT unique_document UNIQUE (document_type_id, document_number);

ALTER TABLE ONLY public.patients
    ADD CONSTRAINT unique_patient_per_tenant UNIQUE (tenant_id, person_id);

ALTER TABLE ONLY public.medical_records
    ADD CONSTRAINT unique_record_per_patient UNIQUE (patient_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT unique_tenant_email UNIQUE (tenant_id, email);

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT unique_user_role UNIQUE (user_id, role_id);

ALTER TABLE ONLY public.doctor_branches
    ADD CONSTRAINT uq_doctor_branch UNIQUE (doctor_id, branch_id);

ALTER TABLE ONLY public.doctor_specialties
    ADD CONSTRAINT uq_doctor_specialty UNIQUE (doctor_id, specialty_id);

ALTER TABLE ONLY public.staff_branches
    ADD CONSTRAINT uq_staff_branch UNIQUE (user_id, branch_id);

ALTER TABLE ONLY public.tenant_appointment_types
    ADD CONSTRAINT uq_tenant_appointment_type UNIQUE (tenant_id, appointment_type_id);

ALTER TABLE ONLY public.tenant_document_types
    ADD CONSTRAINT uq_tenant_document_type UNIQUE (tenant_id, document_type_id);

ALTER TABLE ONLY public.tenant_process_config
    ADD CONSTRAINT uq_tenant_process UNIQUE (tenant_id, process);

ALTER TABLE ONLY public.tenant_specialties
    ADD CONSTRAINT uq_tenant_specialty UNIQUE (tenant_id, specialty_id);

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT uq_tenants_subdomain UNIQUE (subdomain);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email);

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

CREATE INDEX idx_afh_appointment ON public.appointment_flow_history USING btree (appointment_id);

CREATE INDEX idx_appointments_date ON public.appointments USING btree (scheduled_at);

CREATE INDEX idx_appointments_doctor ON public.appointments USING btree (doctor_id);

CREATE INDEX idx_appointments_doctor_scheduled ON public.appointments USING btree (doctor_id, scheduled_at);

CREATE INDEX idx_appointments_flow_status ON public.appointments USING btree (flow_status);

CREATE INDEX idx_appointments_patient ON public.appointments USING btree (patient_id);

CREATE INDEX idx_appt_tenant_branch ON public.appointments USING btree (tenant_id, branch_id);

CREATE INDEX idx_appt_tenant_date ON public.appointments USING btree (tenant_id, scheduled_at);

CREATE INDEX idx_appt_tenant_doctor ON public.appointments USING btree (tenant_id, doctor_id);

CREATE INDEX idx_audit_log_changed_at ON public.audit_log USING btree (changed_at);

CREATE INDEX idx_audit_log_table_record ON public.audit_log USING btree (table_name, record_id);

CREATE INDEX idx_audit_log_tenant ON public.audit_log USING btree (tenant_id);

CREATE INDEX idx_branches_tenant_id ON public.branches USING btree (tenant_id);

CREATE INDEX idx_doctor_branches_branch ON public.doctor_branches USING btree (branch_id);

CREATE INDEX idx_doctor_branches_doctor ON public.doctor_branches USING btree (doctor_id);

CREATE INDEX idx_doctor_specialties_doctor ON public.doctor_specialties USING btree (doctor_id);

CREATE INDEX idx_doctors_tenant_id ON public.doctors USING btree (tenant_id);

CREATE INDEX idx_med_cert_entry ON public.medical_certificates USING btree (medical_entry_id);

CREATE INDEX idx_med_diag_entry ON public.medical_entry_diagnoses USING btree (medical_entry_id);

CREATE INDEX idx_med_order_entry ON public.medical_orders USING btree (medical_entry_id);

CREATE INDEX idx_med_order_result_order ON public.medical_order_results USING btree (medical_order_id);

CREATE INDEX idx_med_proc_entry ON public.medical_procedures USING btree (medical_entry_id);

CREATE INDEX idx_medical_entries_appointment ON public.medical_entries USING btree (appointment_id);

CREATE INDEX idx_medical_entries_record ON public.medical_entries USING btree (medical_record_id);

CREATE INDEX idx_note_templates_doctor ON public.note_templates USING btree (tenant_id, doctor_id);

CREATE INDEX idx_patient_allergies_patient ON public.patient_allergies USING btree (patient_id);

CREATE INDEX idx_patient_conditions_patient ON public.patient_conditions USING btree (patient_id);

CREATE INDEX idx_patient_family_patient ON public.patient_family_history USING btree (patient_id);

CREATE INDEX idx_patient_habits_patient ON public.patient_habits USING btree (patient_id);

CREATE INDEX idx_patient_surgical_patient ON public.patient_surgical_history USING btree (patient_id);

CREATE INDEX idx_patients_tenant ON public.patients USING btree (tenant_id);

CREATE INDEX idx_patients_tenant_id ON public.patients USING btree (tenant_id);

CREATE INDEX idx_payments_appointment ON public.payments USING btree (appointment_id);

CREATE INDEX idx_person_documents_person ON public.person_documents USING btree (person_id);

CREATE INDEX idx_persons_tenant_id ON public.persons USING btree (tenant_id);

CREATE INDEX idx_prescriptions_entry ON public.prescriptions USING btree (medical_entry_id);

CREATE INDEX idx_refresh_tokens_expires ON public.refresh_tokens USING btree (expires_at);

CREATE INDEX idx_refresh_tokens_tenant ON public.refresh_tokens USING btree (tenant_id);

CREATE INDEX idx_refresh_tokens_user ON public.refresh_tokens USING btree (user_id);

CREATE INDEX idx_staff_branches_branch ON public.staff_branches USING btree (branch_id) WHERE (is_active = true);

CREATE INDEX idx_staff_branches_user ON public.staff_branches USING btree (user_id) WHERE (is_active = true);

CREATE INDEX idx_tenant_appt_types_tenant ON public.tenant_appointment_types USING btree (tenant_id);

CREATE INDEX idx_tenant_doc_types_tenant ON public.tenant_document_types USING btree (tenant_id);

CREATE INDEX idx_tenant_process_config_tenant ON public.tenant_process_config USING btree (tenant_id);

CREATE INDEX idx_tenant_specialties_tenant ON public.tenant_specialties USING btree (tenant_id);

CREATE INDEX idx_triage_appointment ON public.triage USING btree (appointment_id);

CREATE INDEX idx_triage_measured_at ON public.triage USING btree (appointment_id, measured_at);

CREATE INDEX idx_users_tenant ON public.users USING btree (tenant_id);

CREATE INDEX idx_users_tenant_id ON public.users USING btree (tenant_id);

CREATE TRIGGER trg_audit_med_diag AFTER INSERT OR DELETE OR UPDATE ON public.medical_entry_diagnoses FOR EACH ROW EXECUTE FUNCTION public.fn_audit_clinical();

CREATE TRIGGER trg_audit_med_orders AFTER INSERT OR DELETE OR UPDATE ON public.medical_orders FOR EACH ROW EXECUTE FUNCTION public.fn_audit_clinical();

CREATE TRIGGER trg_audit_medical_entries AFTER INSERT OR DELETE OR UPDATE ON public.medical_entries FOR EACH ROW EXECUTE FUNCTION public.fn_audit_clinical();

CREATE TRIGGER trg_audit_prescriptions AFTER INSERT OR DELETE OR UPDATE ON public.prescriptions FOR EACH ROW EXECUTE FUNCTION public.fn_audit_clinical();

CREATE TRIGGER trg_audit_triage AFTER INSERT OR DELETE OR UPDATE ON public.triage FOR EACH ROW EXECUTE FUNCTION public.fn_audit_clinical();

CREATE TRIGGER trg_block_locked_entry BEFORE UPDATE ON public.medical_entries FOR EACH ROW WHEN (((old.is_locked = true) AND (new.is_locked = true))) EXECUTE FUNCTION public.fn_block_locked_entry();

ALTER TABLE ONLY public.doctor_specialties
    ADD CONSTRAINT doctor_specialties_doctor_id_fkey FOREIGN KEY (doctor_id) REFERENCES public.doctors(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.doctor_specialties
    ADD CONSTRAINT doctor_specialties_specialty_id_fkey FOREIGN KEY (specialty_id) REFERENCES public.specialties(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.appointment_flow_history
    ADD CONSTRAINT fk_afh_appointment FOREIGN KEY (appointment_id) REFERENCES public.appointments(id);

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT fk_appointments_appointment_type FOREIGN KEY (appointment_type_id) REFERENCES public.appointment_types(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT fk_appointments_branch FOREIGN KEY (branch_id) REFERENCES public.branches(id);

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES public.doctors(id);

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES public.patients(id);

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT fk_appointments_status FOREIGN KEY (status_id) REFERENCES public.appointment_status(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT fk_appointments_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.branches
    ADD CONSTRAINT fk_branches_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants(id);

ALTER TABLE ONLY public.doctor_branches
    ADD CONSTRAINT fk_doctor_branches_branch FOREIGN KEY (branch_id) REFERENCES public.branches(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.doctor_branches
    ADD CONSTRAINT fk_doctor_branches_doctor FOREIGN KEY (doctor_id) REFERENCES public.doctors(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.doctors
    ADD CONSTRAINT fk_doctors_person FOREIGN KEY (person_id) REFERENCES public.persons(id);

ALTER TABLE ONLY public.doctors
    ADD CONSTRAINT fk_doctors_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants(id);

ALTER TABLE ONLY public.doctor_schedules
    ADD CONSTRAINT fk_ds_doctor FOREIGN KEY (doctor_id) REFERENCES public.doctors(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.doctor_schedules
    ADD CONSTRAINT fk_ds_doctor_branch FOREIGN KEY (doctor_branch_id) REFERENCES public.doctor_branches(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.medical_files
    ADD CONSTRAINT fk_files_entry FOREIGN KEY (medical_entry_id) REFERENCES public.medical_entries(id);

ALTER TABLE ONLY public.medical_certificates
    ADD CONSTRAINT fk_med_cert_entry FOREIGN KEY (medical_entry_id) REFERENCES public.medical_entries(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.medical_entry_diagnoses
    ADD CONSTRAINT fk_med_diag_cie10 FOREIGN KEY (cie10_id) REFERENCES public.cie10_codes(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.medical_entry_diagnoses
    ADD CONSTRAINT fk_med_diag_entry FOREIGN KEY (medical_entry_id) REFERENCES public.medical_entries(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.medical_orders
    ADD CONSTRAINT fk_med_order_entry FOREIGN KEY (medical_entry_id) REFERENCES public.medical_entries(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.medical_order_results
    ADD CONSTRAINT fk_med_order_result_order FOREIGN KEY (medical_order_id) REFERENCES public.medical_orders(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.medical_procedures
    ADD CONSTRAINT fk_med_proc_entry FOREIGN KEY (medical_entry_id) REFERENCES public.medical_entries(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.medical_entries
    ADD CONSTRAINT fk_medical_entries_appointment FOREIGN KEY (appointment_id) REFERENCES public.appointments(id);

ALTER TABLE ONLY public.medical_entries
    ADD CONSTRAINT fk_medical_entries_record FOREIGN KEY (medical_record_id) REFERENCES public.medical_records(id);

ALTER TABLE ONLY public.medical_records
    ADD CONSTRAINT fk_medical_record_patient FOREIGN KEY (patient_id) REFERENCES public.patients(id);

ALTER TABLE ONLY public.patient_allergies
    ADD CONSTRAINT fk_patient_allergies_patient FOREIGN KEY (patient_id) REFERENCES public.patients(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.patient_conditions
    ADD CONSTRAINT fk_patient_conditions_cie10 FOREIGN KEY (cie10_id) REFERENCES public.cie10_codes(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.patient_conditions
    ADD CONSTRAINT fk_patient_conditions_patient FOREIGN KEY (patient_id) REFERENCES public.patients(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.patient_family_history
    ADD CONSTRAINT fk_patient_family_patient FOREIGN KEY (patient_id) REFERENCES public.patients(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.patient_habits
    ADD CONSTRAINT fk_patient_habits_patient FOREIGN KEY (patient_id) REFERENCES public.patients(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.patient_surgical_history
    ADD CONSTRAINT fk_patient_surgical_patient FOREIGN KEY (patient_id) REFERENCES public.patients(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.patients
    ADD CONSTRAINT fk_patients_created_by FOREIGN KEY (created_by) REFERENCES public.users(id);

ALTER TABLE ONLY public.patients
    ADD CONSTRAINT fk_patients_person FOREIGN KEY (person_id) REFERENCES public.persons(id);

ALTER TABLE ONLY public.patients
    ADD CONSTRAINT fk_patients_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants(id);

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payments_appointment FOREIGN KEY (appointment_id) REFERENCES public.appointments(id);

ALTER TABLE ONLY public.person_documents
    ADD CONSTRAINT fk_person_documents_document_type FOREIGN KEY (document_type_id) REFERENCES public.document_types(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.person_documents
    ADD CONSTRAINT fk_person_documents_person FOREIGN KEY (person_id) REFERENCES public.persons(id);

ALTER TABLE ONLY public.persons
    ADD CONSTRAINT fk_person_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants(id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants(id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.appointment_reschedules
    ADD CONSTRAINT fk_reschedules_appointment FOREIGN KEY (appointment_id) REFERENCES public.appointments(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT fk_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES public.plans(id);

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT fk_subscriptions_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants(id);

ALTER TABLE ONLY public.triage
    ADD CONSTRAINT fk_triage_appointment FOREIGN KEY (appointment_id) REFERENCES public.appointments(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES public.roles(id);

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_branch FOREIGN KEY (branch_id) REFERENCES public.branches(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_person FOREIGN KEY (person_id) REFERENCES public.persons(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants(id);

ALTER TABLE ONLY public.note_templates
    ADD CONSTRAINT note_templates_doctor_id_fkey FOREIGN KEY (doctor_id) REFERENCES public.doctors(id);

ALTER TABLE ONLY public.note_templates
    ADD CONSTRAINT note_templates_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.tenants(id);

ALTER TABLE ONLY public.prescriptions
    ADD CONSTRAINT prescriptions_medical_entry_id_fkey FOREIGN KEY (medical_entry_id) REFERENCES public.medical_entries(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.staff_branches
    ADD CONSTRAINT staff_branches_branch_id_fkey FOREIGN KEY (branch_id) REFERENCES public.branches(id);

ALTER TABLE ONLY public.staff_branches
    ADD CONSTRAINT staff_branches_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.tenant_appointment_types
    ADD CONSTRAINT tenant_appointment_types_appointment_type_id_fkey FOREIGN KEY (appointment_type_id) REFERENCES public.appointment_types(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tenant_appointment_types
    ADD CONSTRAINT tenant_appointment_types_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tenant_document_types
    ADD CONSTRAINT tenant_document_types_document_type_id_fkey FOREIGN KEY (document_type_id) REFERENCES public.document_types(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tenant_document_types
    ADD CONSTRAINT tenant_document_types_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tenant_process_config
    ADD CONSTRAINT tenant_process_config_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.tenants(id);

ALTER TABLE ONLY public.tenant_specialties
    ADD CONSTRAINT tenant_specialties_specialty_id_fkey FOREIGN KEY (specialty_id) REFERENCES public.specialties(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tenant_specialties
    ADD CONSTRAINT tenant_specialties_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE;

INSERT INTO public.appointment_status (id, code, name, created_at, updated_at, created_by, updated_by, is_active) VALUES (1, 'SCHEDULED', 'Programada', NULL, NULL, NULL, NULL, true);
INSERT INTO public.appointment_status (id, code, name, created_at, updated_at, created_by, updated_by, is_active) VALUES (2, 'CANCELLED', 'Cancelada', NULL, NULL, NULL, NULL, true);
INSERT INTO public.appointment_status (id, code, name, created_at, updated_at, created_by, updated_by, is_active) VALUES (3, 'COMPLETED', 'Completada', NULL, NULL, NULL, NULL, true);
INSERT INTO public.appointment_status (id, code, name, created_at, updated_at, created_by, updated_by, is_active) VALUES (4, 'NO_SHOW', 'No asistió', NULL, NULL, NULL, NULL, true);
INSERT INTO public.appointment_status (id, code, name, created_at, updated_at, created_by, updated_by, is_active) VALUES (5, 'RESCHEDULED', 'Reprogramada', NULL, '2026-05-16 18:43:00.472938', NULL, 1, true);

INSERT INTO public.appointment_types (id, code, name, duration_minutes, is_active, created_at, updated_at, created_by, updated_by) VALUES (1, 'GENERAL_CONSULTATION', 'Consulta General', 30, true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);
INSERT INTO public.appointment_types (id, code, name, duration_minutes, is_active, created_at, updated_at, created_by, updated_by) VALUES (2, 'FOLLOW_UP', 'Control', 15, true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);
INSERT INTO public.appointment_types (id, code, name, duration_minutes, is_active, created_at, updated_at, created_by, updated_by) VALUES (3, 'EMERGENCY', 'Emergencia', 60, true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);
INSERT INTO public.appointment_types (id, code, name, duration_minutes, is_active, created_at, updated_at, created_by, updated_by) VALUES (4, 'PROCEDURE', 'Procedimiento', 45, true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);

INSERT INTO public.document_types (id, code, name, is_active, created_at, updated_at, created_by, updated_by) VALUES (1, 'DNI', 'Documento Nacional de Identidad', true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);
INSERT INTO public.document_types (id, code, name, is_active, created_at, updated_at, created_by, updated_by) VALUES (2, 'PASSPORT', 'Pasaporte', true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);
INSERT INTO public.document_types (id, code, name, is_active, created_at, updated_at, created_by, updated_by) VALUES (3, 'CC', 'Cédula de Ciudadanía', true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);
INSERT INTO public.document_types (id, code, name, is_active, created_at, updated_at, created_by, updated_by) VALUES (4, 'CE', 'Cédula de Extranjería', true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);
INSERT INTO public.document_types (id, code, name, is_active, created_at, updated_at, created_by, updated_by) VALUES (5, 'RUC', 'Registro Único de Contribuyentes', true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);

INSERT INTO public.plans (id, name, code, price, max_users, max_branches, is_active, created_at, updated_at, created_by, updated_by) VALUES (1, 'Básico', 'BASIC', 49.90, 5, 1, true, '2026-04-19 14:25:16.224598', NULL, NULL, NULL);
INSERT INTO public.plans (id, name, code, price, max_users, max_branches, is_active, created_at, updated_at, created_by, updated_by) VALUES (2, 'Profesional', 'PRO', 99.90, 20, 3, true, '2026-04-19 14:25:16.224598', NULL, NULL, NULL);
INSERT INTO public.plans (id, name, code, price, max_users, max_branches, is_active, created_at, updated_at, created_by, updated_by) VALUES (3, 'Premium', 'PREMIUM', 199.90, NULL, NULL, true, '2026-04-19 14:25:16.224598', NULL, NULL, NULL);

INSERT INTO public.roles (id, name, code, is_active, created_at, created_by, updated_at, updated_by) VALUES (1, 'Super Admin', 'SUPER_ADMIN', true, '2026-04-19 13:13:19.778895', NULL, NULL, NULL);
INSERT INTO public.roles (id, name, code, is_active, created_at, created_by, updated_at, updated_by) VALUES (2, 'Clinic Admin', 'CLINIC_ADMIN', true, '2026-04-19 13:13:19.778895', NULL, NULL, NULL);
INSERT INTO public.roles (id, name, code, is_active, created_at, created_by, updated_at, updated_by) VALUES (3, 'Doctor', 'DOCTOR', true, '2026-04-19 13:13:19.778895', NULL, NULL, NULL);
INSERT INTO public.roles (id, name, code, is_active, created_at, created_by, updated_at, updated_by) VALUES (4, 'Assistant', 'ASSISTANT', true, '2026-04-19 13:13:19.778895', NULL, NULL, NULL);
INSERT INTO public.roles (id, name, code, is_active, created_at, created_by, updated_at, updated_by) VALUES (5, 'Receptionist', 'RECEPTIONIST', true, '2026-04-19 13:13:19.778895', NULL, NULL, NULL);
INSERT INTO public.roles (id, name, code, is_active, created_at, created_by, updated_at, updated_by) VALUES (6, 'Patient', 'PATIENT', true, '2026-04-19 13:13:19.778895', NULL, NULL, NULL);

INSERT INTO public.specialties (id, code, name, description, is_active, created_at, updated_at, created_by, updated_by) VALUES (1, 'GENERAL', 'Medicina General', 'Atención médica general', true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);
INSERT INTO public.specialties (id, code, name, description, is_active, created_at, updated_at, created_by, updated_by) VALUES (2, 'PEDIATRICS', 'Pediatría', 'Atención médica infantil', true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);
INSERT INTO public.specialties (id, code, name, description, is_active, created_at, updated_at, created_by, updated_by) VALUES (3, 'CARDIOLOGY', 'Cardiología', 'Especialidad del corazón', true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);
INSERT INTO public.specialties (id, code, name, description, is_active, created_at, updated_at, created_by, updated_by) VALUES (4, 'DERMATOLOGY', 'Dermatología', 'Especialidad de la piel', true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);
INSERT INTO public.specialties (id, code, name, description, is_active, created_at, updated_at, created_by, updated_by) VALUES (5, 'GYNECOLOGY', 'Ginecología', 'Salud femenina', true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);
INSERT INTO public.specialties (id, code, name, description, is_active, created_at, updated_at, created_by, updated_by) VALUES (6, 'TRAUMATOLOGY', 'Traumatología', 'Sistema músculo-esquelético', true, '2026-05-16 09:39:40.015679', NULL, NULL, NULL);

SELECT setval(pg_get_serial_sequence('public.roles', 'id'), COALESCE((SELECT MAX(id) FROM public.roles), 1));
SELECT setval(pg_get_serial_sequence('public.appointment_status', 'id'), COALESCE((SELECT MAX(id) FROM public.appointment_status), 1));
SELECT setval(pg_get_serial_sequence('public.document_types', 'id'), COALESCE((SELECT MAX(id) FROM public.document_types), 1));
SELECT setval(pg_get_serial_sequence('public.plans', 'id'), COALESCE((SELECT MAX(id) FROM public.plans), 1));
SELECT setval(pg_get_serial_sequence('public.specialties', 'id'), COALESCE((SELECT MAX(id) FROM public.specialties), 1));
SELECT setval(pg_get_serial_sequence('public.appointment_types', 'id'), COALESCE((SELECT MAX(id) FROM public.appointment_types), 1));
