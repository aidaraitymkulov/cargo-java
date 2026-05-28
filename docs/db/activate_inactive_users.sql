-- Активировать всех INACTIVE пользователей
-- Запускать только один раз после миграции данных из старой системы

UPDATE users
SET status = 'ACTIVE'
WHERE status = 'INACTIVE';
