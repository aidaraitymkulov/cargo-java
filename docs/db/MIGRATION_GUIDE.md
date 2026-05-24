# Миграция данных: Node.js БД → Java Spring Boot

## Контекст

Продакшн БД была написана на Node.js/TypeORM — схема кардинально отличается от новой Java схемы.
Этот гайд описывает как перенести реальные данные в новую схему.

## Что переносим

| Откуда (старая БД) | Куда (новая схема) | Записей |
|---|---|---|
| `"user"` | `users` | ~50 626 |
| `product` | `products` | ~3 030 622 |
| `news` | `news` | 0 |
| `push_tokens` | `push_tokens` | ~307 |
| `refresh_sessions` | `refresh_sessions` | ~3 183 (только активные) |

Не мигрируем: `confirmation`, `order`, `storage`, `user_role`, `user_roles_user_role`, `product_history`

---

## Шаг 1 — Создать новую локальную БД

В DBeaver:
1. Правой кнопкой на подключение `localhost` в левой панели
2. **Создать → База данных**
3. Имя: `cargo-local`
4. Нажать **OK**

---

## Шаг 2 — Восстановить дамп

1. Правой кнопкой на `cargo-local`
2. **Инструменты → Восстановить**
3. Входной файл: `docs/backupades.sql`
4. Нажать **Старт**

> Займёт 1-3 минуты — в дампе 3.2 млн строк.

---

## Шаг 3 — Переименовать конфликтующие таблицы

Правой кнопкой на `cargo-local` → **Редактор SQL → Открыть редактор SQL**

Выполнить (Ctrl+Enter):

```sql
ALTER TABLE news RENAME TO news_old;
ALTER TABLE push_tokens RENAME TO push_tokens_old;
ALTER TABLE refresh_sessions RENAME TO refresh_sessions_old;
```

> Эти 3 таблицы есть в обеих схемах — переименовываем чтобы не было конфликта при запуске Flyway.

---

## Шаг 4 — Подключить приложение к новой БД

Открыть файл `src/main/resources/application-local.yaml` и изменить строку url:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cargo-local
```

---

## Шаг 5 — Запустить приложение (Flyway создаст новую схему)

```powershell
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
```

В логах должно появиться:
```
Successfully applied 5 migrations to schema "public"
```

После этого остановить приложение **(Ctrl+C)**.

---

## Шаг 6 — Запустить миграцию данных

В DBeaver → SQL Editor на `cargo-local`:
1. **Файл → Открыть файл**
2. Выбрать `docs/db/migrate_data.sql`
3. Нажать **Alt+X** (Execute Script)

В панели Output внизу появится:

```
NOTICE: === Migration complete ===
NOTICE: users:            50626
NOTICE: products:         3030622
NOTICE: news:             0
NOTICE: push_tokens:      307
NOTICE: refresh_sessions: ...
```

---

## Шаг 7 — Проверить данные

В SQL Editor выполнить:

```sql
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM products;
SELECT COUNT(*) FROM push_tokens;
```

---

## Шаг 8 — Удалить старые таблицы

Когда убедишься что данные на месте, выполнить в SQL Editor:

```sql
DROP TABLE IF EXISTS public."user"                CASCADE;
DROP TABLE IF EXISTS public.product               CASCADE;
DROP TABLE IF EXISTS public.product_history       CASCADE;
DROP TABLE IF EXISTS public."order"               CASCADE;
DROP TABLE IF EXISTS public.confirmation          CASCADE;
DROP TABLE IF EXISTS public.storage               CASCADE;
DROP TABLE IF EXISTS public.user_role             CASCADE;
DROP TABLE IF EXISTS public.user_roles_user_role  CASCADE;
DROP TABLE IF EXISTS public.news_old              CASCADE;
DROP TABLE IF EXISTS public.push_tokens_old       CASCADE;
DROP TABLE IF EXISTS public.refresh_sessions_old  CASCADE;

DROP TYPE IF EXISTS public.confirmation_confirmationstatus_enum;
DROP TYPE IF EXISTS public.order_status_enum;
DROP TYPE IF EXISTS public.user_status_enum;
```

---

## Итог

После всех шагов в БД `cargo-local` будет:
- Чистая Java-схема (таблицы `users`, `products`, `news` и т.д.)
- Реальные данные из продакшна
- Приложение подключено через `application-local.yaml`
