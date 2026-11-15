ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS pending_activation boolean NOT NULL DEFAULT false;

ALTER TABLE public.password_reset_tokens
    ADD COLUMN IF NOT EXISTS purpose character varying(20) NOT NULL DEFAULT 'PASSWORD_RESET';

ALTER TABLE public.password_reset_tokens
    DROP CONSTRAINT IF EXISTS chk_password_reset_tokens_purpose;
ALTER TABLE public.password_reset_tokens
    ADD CONSTRAINT chk_password_reset_tokens_purpose CHECK (purpose IN ('PASSWORD_RESET', 'ACTIVATION'));

ALTER TABLE public.persons
    ADD COLUMN IF NOT EXISTS identity_check_failures integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS identity_check_locked_until timestamp without time zone;

UPDATE public.persons SET gender = NULL WHERE gender IS NOT NULL AND btrim(gender) = '';
UPDATE public.persons SET gender = 'M' WHERE upper(btrim(gender)) IN ('M', 'MALE', 'MASCULINO', 'H', 'HOMBRE');
UPDATE public.persons SET gender = 'F' WHERE upper(btrim(gender)) IN ('F', 'FEMALE', 'FEMENINO', 'MUJER');
UPDATE public.persons SET gender = 'OTHER' WHERE gender IS NOT NULL AND gender NOT IN ('M', 'F');

ALTER TABLE public.persons DROP CONSTRAINT IF EXISTS chk_persons_gender;
ALTER TABLE public.persons
    ADD CONSTRAINT chk_persons_gender CHECK (gender IS NULL OR gender IN ('M', 'F', 'OTHER'));

UPDATE public.persons p
   SET profile_completed = (
           coalesce(btrim(p.first_name), '') <> ''
       AND coalesce(btrim(p.last_name), '') <> ''
       AND coalesce(btrim(p.phone), '') <> ''
       AND p.gender IS NOT NULL
       AND p.birth_date IS NOT NULL
       AND EXISTS (SELECT 1 FROM public.person_documents d WHERE d.person_id = p.id));
