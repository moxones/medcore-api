ALTER TABLE public.password_reset_tokens
    ADD COLUMN IF NOT EXISTS attempts integer NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_purpose
    ON public.password_reset_tokens (user_id, purpose, created_at DESC);

CREATE TABLE public.email_verifications (
    id bigserial PRIMARY KEY,
    tenant_id bigint NOT NULL REFERENCES public.tenants (id) ON DELETE CASCADE,
    email character varying(150) NOT NULL,
    code_hash character varying(64) NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    delivered boolean NOT NULL DEFAULT false,
    expires_at timestamp without time zone NOT NULL,
    consumed_at timestamp without time zone,
    requested_ip character varying(64),
    created_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE INDEX idx_email_verifications_lookup
    ON public.email_verifications (tenant_id, email, created_at DESC);

ALTER TABLE public.email_verifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON public.email_verifications
    USING (public.app_tenant_visible(tenant_id))
    WITH CHECK (public.app_tenant_visible(tenant_id));
