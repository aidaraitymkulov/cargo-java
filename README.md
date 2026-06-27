# Cargo App — Backend

Backend логистической системы доставки посылок из Китая в Кыргызстан.

Система обслуживает два клиента: мобильное приложение Flutter (клиенты) и веб-панель React (менеджеры и администраторы). Основной процесс — регистрация посылок в Китае, отслеживание их пути, приёмка в КР и выдача клиентам. Весь поток данных проходит через импорт Excel-файлов.

## Стек

- Java 21 + Spring Boot 4.0
- PostgreSQL + Flyway
- Spring Security + JWT
- Apache POI (Excel)

## Запуск локально

Необходимо: Java 21+, PostgreSQL 14+.

```bash
# 1. Скопировать шаблон переменных окружения
cp .env.example .env
# Заполнить .env своими значениями (DB, JWT, Mail и т.д.)

# 2. Запустить
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

При старте Flyway автоматически применяет миграции.

## Конфигурация

Конфиги берут значения из переменных окружения. Шаблон — `.env.example`.

| Файл | Назначение |
|---|---|
| `application.yaml` | Общие настройки, не изменять |
| `application-dev.yaml` | Локальная разработка, env vars с dev-дефолтами |
| `application-prod.yaml` | Прод, только env vars без дефолтов |

## CI/CD

Деплой запускается автоматически при пуше в `main`.

**Процесс:** GitHub Actions → SSH на сервер → `git pull` → `docker compose up --build`

```
push to main
    └─► GH Actions (deploy-backend.yml)
            └─► SSH → /opt/ades/cargo-java
                    ├─► git pull origin main
                    ├─► docker compose up -d --build --no-deps backend
                    └─► docker image prune -f
```

**Необходимые GitHub Secrets:**

| Secret | Описание |
|---|---|
| `SSH_HOST` | IP или домен сервера |
| `SSH_USER` | Пользователь на сервере |
| `SSH_PRIVATE_KEY` | Приватный SSH-ключ |

**Настройка сервера** (первый раз):
```bash
# Клонировать репо
git clone https://github.com/<org>/cargo-java.git /opt/ades/cargo-java
cd /opt/ades/cargo-java

# Создать .env по шаблону и заполнить prod-значениями
cp .env.example .env

# Запустить
docker compose up -d
```

## Архитектура

Модульный монолит — один деплоируемый артефакт, разбитый на изолированные домены по бизнес-смыслу. Подробнее в `docs/architecture.md`.

```
auth · users · managers · branches · products · orders · imports · news · notifications · chat · reports
```

Два API-фасада:
- **Mobile API** — для Flutter, авторизация через `Authorization: Bearer`
- **Admin API** (`/admin/*`) — для React-панели, авторизация через HttpOnly cookie

## Документация

| Файл | Содержимое |
|---|---|
| `docs/architecture.md` | Архитектурные паттерны и правила кода |
| `docs/auth.md` | Аутентификация, JWT, сессии |
| `docs/branches.md` | Филиалы, personalCode |
| `docs/users.md` | Мобильные пользователи |
| `docs/managers.md` | Менеджеры и SUPER_ADMIN |
| `docs/products.md` | Товары, статусы, история |
| `docs/orders.md` | Заказы, статистика, выручка |
| `docs/imports.md` | Импорт Excel (CN / KG in-kg / KG delivered) |
| `docs/chat.md` | Чат (WebSocket + STOMP, REST API) |
| `docs/database.md` | Схема БД |
| `docs/api-mobile.md` | Эндпоинты Mobile API |
| `docs/api-admin.md` | Эндпоинты Admin API |
| `docs/postman/` | Postman-коллекции по доменам |
