ALTER TABLE public.tenants
    ADD COLUMN theme_preset VARCHAR(30) NOT NULL DEFAULT 'CLASSIC',
    ADD COLUMN accent_color VARCHAR(20),
    ADD COLUMN font_family VARCHAR(30) NOT NULL DEFAULT 'ROBOTO';

ALTER TABLE public.tenants
    ADD CONSTRAINT chk_tenants_theme_preset
        CHECK (theme_preset IN ('CLASSIC', 'OCEAN', 'ROYAL', 'FOREST', 'LAVENDER', 'CORAL', 'AMBER', 'GRAPHITE',
                                'CUSTOM'));

ALTER TABLE public.tenants
    ADD CONSTRAINT chk_tenants_font_family
        CHECK (font_family IN ('ROBOTO', 'INTER', 'POPPINS', 'NUNITO', 'LATO', 'MONTSERRAT'));

UPDATE public.tenants
   SET theme_preset = 'CUSTOM'
 WHERE primary_color ~ '^#[0-9a-fA-F]{6}$';

UPDATE public.tenants
   SET primary_color = NULL
 WHERE primary_color IS NOT NULL
   AND primary_color !~ '^#[0-9a-fA-F]{6}$';

CREATE TABLE public.tenant_logos (
    tenant_id bigint PRIMARY KEY REFERENCES public.tenants (id),
    logo_key character varying(36) NOT NULL UNIQUE,
    content_type character varying(50) NOT NULL,
    content bytea NOT NULL,
    size_bytes integer NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by bigint
);

ALTER TABLE public.tenant_logos ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON public.tenant_logos
    USING (public.app_tenant_visible(tenant_id))
    WITH CHECK (public.app_tenant_visible(tenant_id));
