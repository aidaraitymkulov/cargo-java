-- =============================================================
-- Создать филиалы из префиксов personal_code и привязать юзеров
-- =============================================================

BEGIN;

-- 1. Создать филиалы из уникальных префиксов
INSERT INTO public.branches (
    id,
    address,
    personal_code_prefix,
    is_active,
    next_sequence
)
SELECT
    gen_random_uuid(),
    'Адрес не указан'                                          AS address,
    REGEXP_REPLACE(personal_code, '[0-9]', '', 'g')           AS personal_code_prefix,
    true                                                       AS is_active,
    MAX(REGEXP_REPLACE(personal_code, '[^0-9]', '', 'g')::INT) + 1 AS next_sequence
FROM public.users
WHERE personal_code IS NOT NULL
  AND personal_code ~ '^[A-Za-z]+'
GROUP BY REGEXP_REPLACE(personal_code, '[0-9]', '', 'g');

-- 2. Привязать пользователей к филиалам по префиксу
UPDATE public.users u
SET branch_id = b.id
FROM public.branches b
WHERE u.personal_code IS NOT NULL
  AND REGEXP_REPLACE(u.personal_code, '[0-9]', '', 'g') = b.personal_code_prefix;

-- 3. Итог
DO $$
DECLARE
    v_branches INT;
    v_linked   INT;
BEGIN
    SELECT COUNT(*) INTO v_branches FROM public.branches;
    SELECT COUNT(*) INTO v_linked   FROM public.users WHERE branch_id IS NOT NULL;

    RAISE NOTICE '=== Branches created: %', v_branches;
    RAISE NOTICE '=== Users linked:     %', v_linked;
END $$;

COMMIT;
