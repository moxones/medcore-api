ALTER TABLE public.tenants
    ADD COLUMN business_type VARCHAR(30) NOT NULL DEFAULT 'MEDICAL';

ALTER TABLE public.tenants
    ADD CONSTRAINT chk_tenants_business_type
        CHECK (business_type IN ('MEDICAL', 'PSYCHOLOGY', 'DENTAL', 'NUTRITION', 'THERAPY', 'GENERAL'));
