INSERT INTO public.subscription_status (code, name)
SELECT 'INACTIVE', 'Inactiva'
 WHERE NOT EXISTS (SELECT 1 FROM public.subscription_status WHERE code = 'INACTIVE');
