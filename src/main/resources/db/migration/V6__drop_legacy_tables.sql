-- Удаление старых таблиц Node.js/TypeORM схемы после переноса данных
-- IF EXISTS — безопасно на чистой БД

DROP TABLE IF EXISTS public.user_roles_user_role  CASCADE;
DROP TABLE IF EXISTS public.user_role             CASCADE;
DROP TABLE IF EXISTS public.storage               CASCADE;
DROP TABLE IF EXISTS public.product_history       CASCADE;
DROP TABLE IF EXISTS public."order"               CASCADE;
DROP TABLE IF EXISTS public.product               CASCADE;
DROP TABLE IF EXISTS public."user"                CASCADE;
DROP TABLE IF EXISTS public.confirmation          CASCADE;
DROP TABLE IF EXISTS public.news_old              CASCADE;
DROP TABLE IF EXISTS public.push_tokens_old       CASCADE;
DROP TABLE IF EXISTS public.refresh_sessions_old  CASCADE;

DROP TYPE IF EXISTS public.confirmation_confirmationstatus_enum;
DROP TYPE IF EXISTS public.order_status_enum;
DROP TYPE IF EXISTS public.user_status_enum;
