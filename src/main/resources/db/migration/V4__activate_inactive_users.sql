-- Активировать всех INACTIVE пользователей после переезда с Node.js системы
UPDATE public.users
SET status = 'ACTIVE'
WHERE status = 'INACTIVE';