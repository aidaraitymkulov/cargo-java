-- ====================================================
-- Перенос данных из старой Node.js/TypeORM схемы
-- Защищено IF EXISTS — безопасно на чистой БД
-- ====================================================

DO $$
BEGIN

-- ============================================================
-- USERS  (old: public."user" → new: public.users)
-- ============================================================
IF EXISTS (SELECT FROM information_schema.tables
           WHERE table_schema = 'public' AND table_name = 'user') THEN

    INSERT INTO public.users (
        id, login, email, password_hash,
        first_name, last_name, phone, date_of_birth,
        personal_code, branch_id, chat_banned, status,
        created_at, updated_at
    )
    SELECT
        u.id,
        u.email                                                  AS login,
        u.email,
        u.password                                               AS password_hash,
        u.name                                                   AS first_name,
        u.surname                                                AS last_name,
        u.phone,
        CASE
            WHEN u."dateOfBirth" ~ '^\d{4}-\d{2}-\d{2}$'
             AND SPLIT_PART(u."dateOfBirth", '-', 2)::INT BETWEEN 1 AND 12
             AND SPLIT_PART(u."dateOfBirth", '-', 3)::INT BETWEEN 1 AND 31
            THEN u."dateOfBirth"::DATE
            ELSE '1970-01-01'::DATE
        END                                                      AS date_of_birth,
        u.personal_code,
        NULL                                                     AS branch_id,
        false                                                    AS chat_banned,
        CASE u.status::text
            WHEN '0' THEN 'ACTIVE'
            WHEN '1' THEN 'INACTIVE'
            WHEN '2' THEN 'DELETED'
            WHEN '3' THEN 'PENDING_DELETION'
            ELSE 'ACTIVE'
        END                                                      AS status,
        u."dateCreated"                                          AS created_at,
        u."dateUpdated"                                          AS updated_at
    FROM public."user" u
    ON CONFLICT (id) DO NOTHING;

    RAISE NOTICE 'users migrated: %', (SELECT COUNT(*) FROM public.users);
ELSE
    RAISE NOTICE 'Table "user" not found — skipping users migration';
END IF;

-- ============================================================
-- PRODUCTS  (old: public.product → new: public.products)
-- ============================================================
IF EXISTS (SELECT FROM information_schema.tables
           WHERE table_schema = 'public' AND table_name = 'product') THEN

    INSERT INTO public.products (
        id, hatch, user_id, status, price, weight, created_at, updated_at
    )
    SELECT
        p.id,
        p.hatch,
        p."userId"                                               AS user_id,
        CASE UPPER(p.status)
            WHEN 'IN_CHINA'   THEN 'IN_CHINA'
            WHEN 'ON_THE_WAY' THEN 'ON_THE_WAY'
            WHEN 'IN_KG'      THEN 'IN_KG'
            WHEN 'DELIVERED'  THEN 'DELIVERED'
            ELSE 'IN_CHINA'
        END                                                      AS status,
        NULL                                                     AS price,
        NULL                                                     AS weight,
        p."dateCreated"                                          AS created_at,
        p."dateUpdated"                                          AS updated_at
    FROM public.product p
    WHERE p."userId" IS NOT NULL
      AND EXISTS (SELECT 1 FROM public.users u WHERE u.id = p."userId")
    ON CONFLICT (id) DO NOTHING;

    RAISE NOTICE 'products migrated: %', (SELECT COUNT(*) FROM public.products);
ELSE
    RAISE NOTICE 'Table "product" not found — skipping products migration';
END IF;

-- ============================================================
-- NEWS  (old: public.news_old → new: public.news)
-- ============================================================
IF EXISTS (SELECT FROM information_schema.tables
           WHERE table_schema = 'public' AND table_name = 'news_old') THEN

    INSERT INTO public.news (id, image, title, content, created_at, updated_at)
    SELECT
        n.id,
        n.cover AS image,
        n.title,
        n.content,
        n."dateCreated" AS created_at,
        n."dateUpdated" AS updated_at
    FROM public.news_old n
    ON CONFLICT (id) DO NOTHING;

    RAISE NOTICE 'news migrated: %', (SELECT COUNT(*) FROM public.news);
ELSE
    RAISE NOTICE 'Table "news_old" not found — skipping news migration';
END IF;

-- ============================================================
-- PUSH TOKENS  (old: public.push_tokens_old → new: public.push_tokens)
-- ============================================================
IF EXISTS (SELECT FROM information_schema.tables
           WHERE table_schema = 'public' AND table_name = 'push_tokens_old') THEN

    INSERT INTO public.push_tokens (user_id, token, created_at, updated_at)
    SELECT
        pt."userId" AS user_id,
        pt.token,
        pt.created_at,
        pt.updated_at
    FROM public.push_tokens_old pt
    WHERE pt."userId" IS NOT NULL
      AND EXISTS (SELECT 1 FROM public.users u WHERE u.id = pt."userId")
    ON CONFLICT DO NOTHING;

    RAISE NOTICE 'push_tokens migrated: %', (SELECT COUNT(*) FROM public.push_tokens);
ELSE
    RAISE NOTICE 'Table "push_tokens_old" not found — skipping push_tokens migration';
END IF;

-- ============================================================
-- REFRESH SESSIONS  (old: public.refresh_sessions_old → new)
-- Только активные (не отозванные и не истёкшие)
-- ============================================================
IF EXISTS (SELECT FROM information_schema.tables
           WHERE table_schema = 'public' AND table_name = 'refresh_sessions_old') THEN

    INSERT INTO public.refresh_sessions (
        id, user_id, jti, fingerprint, ip, user_agent,
        created_at, expires_at, revoked_at
    )
    SELECT
        rs.id,
        rs."userId"    AS user_id,
        rs.jti,
        NULL           AS fingerprint,
        rs.ip,
        rs."userAgent" AS user_agent,
        rs."createdAt" AS created_at,
        rs."expiresAt" AS expires_at,
        rs."revokedAt" AS revoked_at
    FROM public.refresh_sessions_old rs
    WHERE rs."userId" IS NOT NULL
      AND rs."revokedAt" IS NULL
      AND rs."expiresAt" > now()
      AND EXISTS (SELECT 1 FROM public.users u WHERE u.id = rs."userId")
    ON CONFLICT (jti) DO NOTHING;

    RAISE NOTICE 'refresh_sessions migrated: %', (SELECT COUNT(*) FROM public.refresh_sessions);
ELSE
    RAISE NOTICE 'Table "refresh_sessions_old" not found — skipping sessions migration';
END IF;

END $$;
