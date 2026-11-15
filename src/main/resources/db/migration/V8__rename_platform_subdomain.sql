-- La organización de plataforma (solo super administrador) pasa de 'healthcare' a 'admin'.
UPDATE public.tenants
SET subdomain = 'admin'
WHERE subdomain = 'healthcare'
  AND NOT EXISTS (SELECT 1 FROM public.tenants WHERE subdomain = 'admin');
