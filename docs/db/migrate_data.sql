-- =============================================================
-- DATA MIGRATION: Node.js/TypeORM schema → Java Spring Boot schema
-- =============================================================
--
-- КАК ЗАПУСКАТЬ:
--
-- Шаг 1: Залить дамп в локальную БД
--   psql -U postgres -d cargo-app -f docs/backupades.sql
--
-- Шаг 2: Переименовать конфликтующие таблицы
--   psql -U postgres -d cargo-app -c "
--     ALTER TABLE news RENAME TO news_old;
--     ALTER TABLE push_tokens RENAME TO push_tokens_old;
--     ALTER TABLE refresh_sessions RENAME TO refresh_sessions_old;
--   "
--
-- Шаг 3: Запустить приложение (Flyway применит V1-V5)
--   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
--
-- Шаг 4: Запустить этот скрипт
--   psql -U postgres -d cargo-app -f docs/db/migrate_data.sql
--
-- =============================================================

BEGIN;

-- ============================================================
-- 1. USERS
--    old: public."user"  →  new: public.users
--
--    name/surname      → first_name/last_name
--    email             → login  (в старой системе auth через email)
--    password          → password_hash  (уже bcrypt)
--    "dateOfBirth"     → date_of_birth  (VARCHAR → DATE, safe cast)
--    status enum '0'-'3' → ACTIVE/INACTIVE/DELETED/PENDING_DELETION
--    branch_id         → NULL (нет филиалов в старой системе)
-- ============================================================
INSERT INTO public.users (
    id,
    login,
    email,
    password_hash,
    first_name,
    last_name,
    phone,
    date_of_birth,
    personal_code,
    branch_id,
    chat_banned,
    status,
    created_at,
    updated_at
)
SELECT
    u.id,
    u.email                                              AS login,
    u.email,
    u.password                                           AS password_hash,
    u.name                                               AS first_name,
    u.surname                                            AS last_name,
    u.phone,
    CASE
        WHEN u."dateOfBirth" ~ '^\d{4}-\d{2}-\d{2}$'
        THEN (
            CASE
                WHEN SPLIT_PART(u."dateOfBirth", '-', 2)::INT BETWEEN 1 AND 12
             AND SPLIT_PART(u."dateOfBirth", '-', 3)::INT BETWEEN 1 AND 31
            THEN u."dateOfBirth"::DATE
            ELSE '1970-01-01'::DATE
        END
            )
        ELSE '1970-01-01'::DATE
    END                                                  AS date_of_birth,
    u.personal_code,
    NULL                                                 AS branch_id,
    false                                                AS chat_banned,
    CASE u.status::text
        WHEN '0' THEN 'ACTIVE'
        WHEN '1' THEN 'INACTIVE'
        WHEN '2' THEN 'DELETED'
        WHEN '3' THEN 'PENDING_DELETION'
        ELSE 'ACTIVE'
    END                                                  AS status,
    u."dateCreated"                                      AS created_at,
    u."dateUpdated"                                      AS updated_at
FROM public."user" u
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 2. PRODUCTS
--    old: public.product  →  new: public.products
--
--    "userId"  → user_id
--    status    → status (UPPER-cast для безопасности)
--    price/weight → NULL (в старой схеме были на order, не на product)
--    "orderId" → не мигрируем (orders удалены в V5)
--
--    Фильтр: только продукты с существующим user_id
-- ============================================================
INSERT INTO public.products (
    id,
    hatch,
    user_id,
    status,
    price,
    weight,
    created_at,
    updated_at
)
SELECT
    p.id,
    p.hatch,
    p."userId"                                           AS user_id,
    CASE UPPER(p.status)
        WHEN 'IN_CHINA'   THEN 'IN_CHINA'
        WHEN 'ON_THE_WAY' THEN 'ON_THE_WAY'
        WHEN 'IN_KG'      THEN 'IN_KG'
        WHEN 'DELIVERED'  THEN 'DELIVERED'
        ELSE 'IN_CHINA'
    END                                                  AS status,
    NULL                                                 AS price,
    NULL                                                 AS weight,
    p."dateCreated"                                      AS created_at,
    p."dateUpdated"                                      AS updated_at
FROM public.product p
WHERE p."userId" IS NOT NULL
  AND EXISTS (SELECT 1 FROM public.users u WHERE u.id = p."userId")
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 3. NEWS
--    old: public.news_old  →  new: public.news
--
--    cover → image (переименована в V2)
-- ============================================================
INSERT INTO public.news (
    id,
    image,
    title,
    content,
    created_at,
    updated_at
)
SELECT
    n.id,
    n.image                                              AS image,
    n.title,
    n.content,
    n."dateCreated"                                      AS created_at,
    n."dateUpdated"                                      AS updated_at
FROM public.news_old n
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 4. PUSH TOKENS
--    old: public.push_tokens_old  →  new: public.push_tokens
--
--    id не мигрируем — SERIAL, генерируется автоматически
--    Фильтр: только токены с существующим user_id
-- ============================================================
INSERT INTO public.push_tokens (
    user_id,
    token,
    created_at,
    updated_at
)
SELECT
    pt."userId"                                          AS user_id,
    pt.token,
    pt.created_at,
    pt.updated_at
FROM public.push_tokens_old pt
WHERE pt."userId" IS NOT NULL
  AND EXISTS (SELECT 1 FROM public.users u WHERE u.id = pt."userId")
ON CONFLICT DO NOTHING;

-- ============================================================
-- 5. REFRESH SESSIONS
--    old: public.refresh_sessions_old  →  new: public.refresh_sessions
--
--    Мигрируем только активные (не отозванные и не истёкшие)
--    fingerprint → NULL (в старой схеме не было)
-- ============================================================
INSERT INTO public.refresh_sessions (
    id,
    user_id,
    jti,
    fingerprint,
    ip,
    user_agent,
    created_at,
    expires_at,
    revoked_at
)
SELECT
    rs.id,
    rs."userId"                                          AS user_id,
    rs.jti,
    NULL                                                 AS fingerprint,
    rs.ip,
    rs."userAgent"                                       AS user_agent,
    rs."createdAt"                                       AS created_at,
    rs."expiresAt"                                       AS expires_at,
    rs."revokedAt"                                       AS revoked_at
FROM public.refresh_sessions_old rs
WHERE rs."userId" IS NOT NULL
  AND rs."revokedAt" IS NULL
  AND rs."expiresAt" > now()
  AND EXISTS (SELECT 1 FROM public.users u WHERE u.id = rs."userId")
ON CONFLICT (jti) DO NOTHING;

-- ============================================================
-- ИТОГ — проверка количества перенесённых записей
-- ============================================================
DO $$
DECLARE
    v_users    INT;
    v_products INT;
    v_news     INT;
    v_tokens   INT;
    v_sessions INT;
BEGIN
    SELECT COUNT(*) INTO v_users    FROM public.users;
    SELECT COUNT(*) INTO v_products FROM public.products;
    SELECT COUNT(*) INTO v_news     FROM public.news;
    SELECT COUNT(*) INTO v_tokens   FROM public.push_tokens;
    SELECT COUNT(*) INTO v_sessions FROM public.refresh_sessions;

    RAISE NOTICE '=== Migration complete ===';
    RAISE NOTICE 'users:            %', v_users;
    RAISE NOTICE 'products:         %', v_products;
    RAISE NOTICE 'news:             %', v_news;
    RAISE NOTICE 'push_tokens:      %', v_tokens;
    RAISE NOTICE 'refresh_sessions: %', v_sessions;
END $$;

COMMIT;

-- ============================================================
-- CLEANUP — удалить старые таблицы (запускать после проверки данных)
-- ============================================================
-- DROP TABLE IF EXISTS public."user"                    CASCADE;
-- DROP TABLE IF EXISTS public.product                   CASCADE;
-- DROP TABLE IF EXISTS public.product_history           CASCADE;
-- DROP TABLE IF EXISTS public."order"                   CASCADE;
-- DROP TABLE IF EXISTS public.confirmation              CASCADE;
-- DROP TABLE IF EXISTS public.storage                   CASCADE;
-- DROP TABLE IF EXISTS public.user_role                 CASCADE;
-- DROP TABLE IF EXISTS public.user_roles_user_role      CASCADE;
-- DROP TABLE IF EXISTS public.news_old                  CASCADE;
-- DROP TABLE IF EXISTS public.push_tokens_old           CASCADE;
-- DROP TABLE IF EXISTS public.refresh_sessions_old      CASCADE;
-- DROP TYPE  IF EXISTS public.confirmation_confirmationstatus_enum;
-- DROP TYPE  IF EXISTS public.order_status_enum;
-- DROP TYPE  IF EXISTS public.user_status_enum;
