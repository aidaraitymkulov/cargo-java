-- ====================================================
-- Создать филиалы из префиксов personal_code и привязать пользователей
-- Безопасно на чистой БД — если нет пользователей с personal_code, ничего не сделает
-- ====================================================

INSERT INTO public.branches (
    id,
    address,
    personal_code_prefix,
    next_sequence
)
SELECT
    gen_random_uuid(),
    'Адрес не указан'                                                    AS address,
    REGEXP_REPLACE(personal_code, '[0-9]', '', 'g')                      AS personal_code_prefix,
    MAX(REGEXP_REPLACE(personal_code, '[^0-9]', '', 'g')::INT) + 1       AS next_sequence
FROM public.users
WHERE personal_code IS NOT NULL
  AND personal_code ~ '^[A-Za-z]+'
  AND REGEXP_REPLACE(personal_code, '[0-9]', '', 'g') != 'SUPERADMIN'
GROUP BY REGEXP_REPLACE(personal_code, '[0-9]', '', 'g')
ON CONFLICT DO NOTHING;

UPDATE public.users u
SET branch_id = b.id
FROM public.branches b
WHERE u.personal_code IS NOT NULL
  AND REGEXP_REPLACE(u.personal_code, '[0-9]', '', 'g') = b.personal_code_prefix
  AND u.branch_id IS NULL;