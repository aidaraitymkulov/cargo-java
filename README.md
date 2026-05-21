# Cargo App — Backend

Backend логистической системы доставки посылок из Китая в Кыргызстан.

Система обслуживает два клиента: мобильное приложение Flutter (клиенты) и веб-панель React (менеджеры и администраторы). Основной процесс — регистрация посылок в Китае, отслеживание их пути, приёмка в КР и выдача клиентам. Весь поток данных проходит через импорт Excel-файлов.

## Стек

- Java 21 + Spring Boot 4.0
- PostgreSQL + Flyway
- Spring Security + JWT
- Apache POI (Excel)

## Запуск

Необходимо: Java 21+, PostgreSQL 14+.

Перед запуском нужно создать базу данных и заполнить `application-dev.yaml` — параметры подключения к БД, JWT-секреты и путь для загрузки файлов.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

При старте Flyway автоматически применяет миграции. Профиль `local` не существует — для локальной разработки используется `dev`.

## Конфигурация

| Файл | Назначение |
|---|---|
| `application.yaml` | Общие настройки, не изменять |
| `application-dev.yaml` | Локальная разработка |
| `application-prod.yaml` | Прод, параметры из env-переменных |

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
| `docs/database.md` | Схема БД |
| `docs/api-mobile.md` | Эндпоинты Mobile API |
| `docs/api-admin.md` | Эндпоинты Admin API |
| `docs/postman/` | Postman-коллекции по доменам |
