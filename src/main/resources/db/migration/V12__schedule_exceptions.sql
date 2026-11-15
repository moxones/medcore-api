CREATE TABLE public.schedule_exceptions (
    id bigserial PRIMARY KEY,
    tenant_id bigint NOT NULL REFERENCES public.tenants (id),
    doctor_id bigint NOT NULL REFERENCES public.doctors (id),
    branch_id bigint REFERENCES public.branches (id),
    kind character varying(10) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    start_time time without time zone,
    end_time time without time zone,
    slot_duration_minutes integer,
    reason character varying(30) NOT NULL,
    note character varying(255),
    status character varying(15) NOT NULL DEFAULT 'APPROVED',
    reviewed_by bigint,
    reviewed_at timestamp without time zone,
    review_note character varying(255),
    cancelled_by bigint,
    cancelled_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp without time zone,
    updated_by bigint,
    CONSTRAINT chk_se_kind CHECK (kind IN ('BLOCK', 'EXTRA')),
    CONSTRAINT chk_se_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_se_reason CHECK (reason IN ('VACATION', 'SICK_LEAVE', 'PERSONAL', 'TRAINING',
                                               'UNPLANNED_ABSENCE', 'EXTRA_HOURS', 'OTHER')),
    CONSTRAINT chk_se_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_se_times CHECK ((start_time IS NULL AND end_time IS NULL)
                                   OR (start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time)),
    CONSTRAINT chk_se_extra CHECK (kind = 'BLOCK'
                                   OR (start_time IS NOT NULL AND branch_id IS NOT NULL)),
    CONSTRAINT chk_se_slot CHECK (slot_duration_minutes IS NULL
                                  OR slot_duration_minutes BETWEEN 5 AND 480)
);

CREATE INDEX idx_schedule_exceptions_doctor_dates
    ON public.schedule_exceptions (doctor_id, start_date, end_date);
CREATE INDEX idx_schedule_exceptions_tenant_status
    ON public.schedule_exceptions (tenant_id, status);

CREATE TRIGGER trg_schedule_exceptions_tenant
    BEFORE INSERT OR UPDATE OF doctor_id ON public.schedule_exceptions
    FOR EACH ROW EXECUTE FUNCTION public.fn_inherit_tenant('doctors', 'doctor_id');

ALTER TABLE public.schedule_exceptions ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON public.schedule_exceptions
    USING (public.app_tenant_visible(tenant_id))
    WITH CHECK (public.app_tenant_visible(tenant_id));

CREATE TABLE public.branch_closures (
    id bigserial PRIMARY KEY,
    tenant_id bigint NOT NULL REFERENCES public.tenants (id),
    branch_id bigint REFERENCES public.branches (id),
    closure_date date NOT NULL,
    name character varying(120) NOT NULL,
    source character varying(20) NOT NULL DEFAULT 'MANUAL',
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp without time zone,
    updated_by bigint,
    CONSTRAINT chk_bc_source CHECK (source IN ('MANUAL', 'NATIONAL_HOLIDAY'))
);

CREATE UNIQUE INDEX uq_branch_closures_day
    ON public.branch_closures (tenant_id, COALESCE(branch_id, 0), closure_date);

ALTER TABLE public.branch_closures ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON public.branch_closures
    USING (public.app_tenant_visible(tenant_id))
    WITH CHECK (public.app_tenant_visible(tenant_id));

ALTER TABLE public.appointments
    ADD COLUMN needs_reschedule boolean NOT NULL DEFAULT false,
    ADD COLUMN reschedule_cause character varying(30),
    ADD COLUMN reschedule_note character varying(255),
    ADD COLUMN reschedule_flagged_at timestamp without time zone,
    ADD COLUMN schedule_exception_id bigint REFERENCES public.schedule_exceptions (id) ON DELETE SET NULL,
    ADD COLUMN branch_closure_id bigint REFERENCES public.branch_closures (id) ON DELETE SET NULL,
    ADD COLUMN cancellation_origin character varying(20);

ALTER TABLE public.appointments
    ADD CONSTRAINT chk_appointments_reschedule_cause
        CHECK (reschedule_cause IS NULL OR reschedule_cause IN ('PROFESSIONAL_ABSENCE', 'BRANCH_CLOSURE'));

ALTER TABLE public.appointments
    ADD CONSTRAINT chk_appointments_cancellation_origin
        CHECK (cancellation_origin IS NULL OR cancellation_origin IN ('PATIENT', 'PROFESSIONAL', 'ORGANIZATION'));

CREATE INDEX idx_appointments_needs_reschedule
    ON public.appointments (tenant_id, needs_reschedule)
    WHERE needs_reschedule;
