# Admin API (React)

Base URL: `https://api.adesexpress.com/admin`

## Аутентификация
- JWT в **HttpOnly Secure cookie** (устанавливается при логине)
- Все запросы с `credentials: 'include'`
- Заголовок `Authorization` не используется

## Роли
- `MANAGER` — базовый доступ (пользователи, импорт, чат, уведомления, чтение новостей)
- `SUPER_ADMIN` — полный доступ (+ менеджеры, филиалы, отчёты по пользователям)

## Модели

### User (Admin)
```json
{
  "id": "uuid", "role": "SUPER_ADMIN | MANAGER | USER",
  "email": "...", "firstName": "...", "lastName": "...", "phone": "...",
  "personalCode": "AN0001", "branch": Branch,
  "chatBanned": false, "status": 0,
  "createdAt": "ISO", "updatedAt": "ISO"
}
```

### Manager
```json
{
  "id": "uuid", "login": "manager01",
  "firstName": "Айгуль", "lastName": "Сатыбалдиева", "phone": "+996700000010",
  "role": "MANAGER | SUPER_ADMIN", "branch": Branch,
  "createdAt": "ISO"
}
```

### Branch (Admin — полная)
```json
{
  "id": "uuid", "address": "г. Бишкек, Анкара-10",
  "personalCodePrefix": "AN", "isActive": true,
  "nextSequence": 42, "createdAt": "ISO", "updatedAt": "ISO"
}
```

### Product
```json
{ "id": "uuid", "hatch": "YT123", "userId": "uuid", "firstName": "...", "lastName": "...",
  "personalCode": "AN0001", "status": "IN_CHINA | ON_THE_WAY | IN_KG | DELIVERED",
  "price": 5000.00, "weight": 3.0, "createdAt": "ISO", "updatedAt": "ISO" }
```
`price` и `weight` — опциональны, могут быть `null`.

---

## Auth

### POST /auth/login (общий с Mobile)
Заголовок `X-Client-Type: web` — токены идут в cookie.
```json
// Request
{ "login": "admin@example.com", "password": "StrongP@ssw0rd" }
// Response 200
{ "success": true, "user": User }
```

### POST /auth/refresh — обновить cookie-токен
### POST /auth/logout — очистить cookie

---

## Менеджеры — `/admin/managers` (SUPER_ADMIN)

### POST /admin/managers
```json
// Request
{ "login": "manager01", "password": "StrongP@ssw0rd",
  "firstName": "Айгуль", "lastName": "Сатыбалдиева", "phone": "+996777000000",
  "branchId": "uuid" }
// Response 201: Manager
// Errors: 400 VALIDATION_ERROR, 403 FORBIDDEN, 409 CONFLICT, 404 BRANCH_NOT_FOUND
```

### GET /admin/managers
```json
// Response 200: [Manager]
```

### GET /admin/managers/{managerId}
```json
// Response 200: Manager
// Errors: 404 NOT_FOUND
```

### PATCH /admin/managers/{managerId}
Все поля опциональны. Если передан `password` — хешируется и сохраняется (сброс пароля).
```json
// Request
{ "login": "...", "password": "NewPass456",
  "firstName": "...", "lastName": "...", "phone": "...", "branchId": "uuid" }
// Response 200: Manager
// Errors: 401, 403 FORBIDDEN, 404 NOT_FOUND, 400 VALIDATION_ERROR, 409 CONFLICT
```

### DELETE /admin/managers/{managerId}
Response 204. Errors: 401, 403, 404 NOT_FOUND, 409 CONFLICT (нельзя удалить себя)

---

## Филиалы — `/admin/branches` (SUPER_ADMIN)

### POST /admin/branches
```json
// Request
{ "address": "г. Бишкек, Анкара-10", "personalCodePrefix": "D" }
// Response 201: Branch
// Errors: 400 VALIDATION_ERROR, 409 CONFLICT (prefix занят)
```

### GET /admin/branches
```json
// Response 200
{ "items": [Branch], "page": 1, "pageSize": 50, "total": 5 }
```

### GET /admin/branches/{branchId} → Branch
Errors: 404 BRANCH_NOT_FOUND

### PATCH /admin/branches/{branchId}
```json
// Request (все опциональные)
{ "address": "...", "personalCodePrefix": "AN", "isActive": true }
// Response 200: Branch
// Errors: 404 BRANCH_NOT_FOUND, 409 CONFLICT
```

### PATCH /admin/branches/{branchId}/activate → Branch
### PATCH /admin/branches/{branchId}/deactivate → Branch
Без тела. Errors: 404 BRANCH_NOT_FOUND

---

## Пользователи — `/admin/users` (MANAGER + SUPER_ADMIN)

### GET /admin/users
Query: `prefix`, `code`, `branchId`, `role`, `page`, `pageSize`

> MANAGER всегда видит только пользователей своего филиала (параметр `branchId` игнорируется).
> SUPER_ADMIN без `branchId` — все пользователи; с `branchId` — конкретный филиал.

```json
// Response 200
{ "items": [User], "page": 1, "pageSize": 20, "total": 123 }
```

### GET /admin/users/stats
Query: `branchId` (опционально, только для SUPER_ADMIN)

> MANAGER всегда получает статистику по своему филиалу.
> SUPER_ADMIN без `branchId` — по всему приложению; с `branchId` — по конкретному филиалу.

```json
// Response 200
{ "total": 312, "newThisMonth": 24 }
```

### GET /admin/users/{userId} → User
Errors: 404 USER_NOT_FOUND

### DELETE /admin/users/{userId}
Response 204. Errors: 404 USER_NOT_FOUND

### PATCH /admin/users/{userId}/chat-ban → User (без тела)
### PATCH /admin/users/{userId}/chat-unban → User (без тела)
Errors: 404 USER_NOT_FOUND

### GET /admin/users/{userId}/products
```json
// Response 200
{ "items": [Product], "page": 1, "pageSize": 50, "total": 10 }
```

---

## Дашборд — `/admin/dashboard` (MANAGER + SUPER_ADMIN)

### GET /admin/dashboard/summary
Query: `branchId` (опционально, только SUPER_ADMIN)

> MANAGER всегда видит только свой филиал. SUPER_ADMIN без `branchId` — всё приложение.
> Поле `revenueThisWeek` присутствует только для SUPER_ADMIN (иначе `null`).

```json
// Response 200
{
  "totalUsers": 312,
  "newUsersThisMonth": 24,
  "productsInChina": 120,
  "productsOnTheWay": 45,
  "productsAwaitingPickup": 23,
  "revenueThisWeek": 45000.00
}
```

### GET /admin/dashboard/charts/users
Query: `from` (ISO date), `to` (ISO date), `branchId` (опционально)
Новые пользователи по дням за произвольный период. Дни с нулём включены.
```json
// Response 200
[
  { "date": "2026-05-01", "count": 5 },
  { "date": "2026-05-02", "count": 0 },
  { "date": "2026-05-24", "count": 3 }
]
```

### GET /admin/dashboard/charts/products/in-china
Query: `from`, `to`, `branchId` (опционально)
Товары, получившие статус `IN_CHINA` в каждый день периода.

### GET /admin/dashboard/charts/products/on-the-way
Query: `from`, `to`, `branchId` (опционально)
Товары, получившие статус `ON_THE_WAY` в каждый день периода.

### GET /admin/dashboard/charts/products/delivered
Query: `from`, `to`, `branchId` (опционально)
Товары, выданные (`DELIVERED`) в каждый день периода.

Все графики возвращают формат `[{ "date": "ISO", "count": N }]` с нулями за дни без данных.

---

## Товары — `/admin/products` (MANAGER + SUPER_ADMIN)

### GET /admin/products/stats
Query: `branchId` (опционально, только для SUPER_ADMIN)
Количество товаров со статусом `ON_THE_WAY`.
> MANAGER — только свой филиал. SUPER_ADMIN без `branchId` — всё приложение.
```json
// Response 200
{ "count": 230 }
```

---

## Импорт Excel — `/admin/import` (MANAGER + SUPER_ADMIN)

Количество загружаемых файлов в день не ограничено. Каждый обрабатывается независимо.
MANAGER — только файлы своего филиала. SUPER_ADMIN — любой.

### POST /admin/import/parcels/cn
Body: `multipart/form-data`, поле `file` (Excel)
Колонки: A=hatch, B=personalCode
```json
// Response 200
{ "processed": 120, "createdProducts": 118, "updatedProducts": 0,
  "errors": [{ "row": 15, "code": "USER_NOT_FOUND", "message": "..." }] }
// HTTP Errors: 400 VALIDATION_ERROR, 413 PAYLOAD_TOO_LARGE, 500 IMPORT_ERROR
```

### POST /admin/import/parcels/kg/{personalCodePrefix}/in-kg
Body: `multipart/form-data`, поле `file` (Excel)
Колонки: A=personalCode (итог), B=hatch, C=вес (итог), D=цена (итог)
Устанавливает `price` и `weight` на каждый товар блока, статус → `IN_KG`.
```json
// Response 200
{ "processed": 20,
  "errors": [{ "row": 12, "code": "USER_NOT_FOUND", "message": "..." }] }
// HTTP Errors:
// 400 VALIDATION_ERROR
// 403 FORBIDDEN — менеджер загружает не свой филиал
// 404 BRANCH_NOT_FOUND
// 500 IMPORT_ERROR
```

### POST /admin/import/parcels/kg/{personalCodePrefix}/delivered
Тот же формат файла. Переводит товары в `DELIVERED` (поиск по hatch + статус `IN_KG`).
```json
// Response 200
{ "processed": 15,
  "errors": [{ "row": 8, "code": "PRODUCT_NOT_FOUND", "message": "..." }] }
// HTTP Errors: 400, 403, 404, 500 (аналогично in-kg)
```

---

## Новости — `/admin/news`

### News (модель)
```json
{ "id": "uuid", "image": "url", "title": "...", "content": "...", "createdAt": "ISO", "updatedAt": "ISO" }
```

### POST /admin/news
Body: `multipart/form-data`
- `title` (text, обязательно)
- `content` (text, обязательно)
- `image` (file, обязательно, только image/*)
```json
// Response 201: News
// Errors: 400 VALIDATION_ERROR, 413 FILE_TOO_LARGE, 415 UNSUPPORTED_MEDIA_TYPE
```

### GET /admin/news → { items: [News], page, pageSize, total }
### GET /admin/news/{newsId} → News (404 NEWS_NOT_FOUND)

### PATCH /admin/news/{newsId}
Body: `multipart/form-data` (все поля опциональны)
- `title` (text)
- `content` (text)
- `image` (file, только image/*)
```json
// Response 200: News
// Errors: 404 NEWS_NOT_FOUND, 400 VALIDATION_ERROR, 415 UNSUPPORTED_MEDIA_TYPE
```

### DELETE /admin/news/{newsId}
Response 204. Errors: 404 NEWS_NOT_FOUND

---

## Push-уведомления — `/admin/notifications`

### POST /admin/notifications/test
```json
// Request
{ "userId": "uuid", "title": "...", "body": "...", "data": { "type": "TEST" } }
// Response 200: { "success": true, "notificationId": "uuid" }
// Errors: 404 USER_NOT_FOUND, 400 VALIDATION_ERROR
```

### POST /admin/notifications/broadcast
```json
// Request
{ "title": "...", "body": "...", "data": { "type": "BROADCAST" } }
// Response 200: { "success": true, "sentCount": 1234 }
```

### POST /admin/notifications/by-personal-code
```json
// Request
{ "codePrefix": "AN", "title": "...", "body": "...", "data": { "type": "BRANCH_INFO" } }
// Response 200: { "success": true, "sentCount": 250 }
// Errors: 400 VALIDATION_ERROR
```

---

## Чат — `/admin/chats`

### GET /admin/chats/conversations
```json
// Response 200
{ "items": [{ "user": User, "lastMessage": ChatMessage,
              "lastMessageAt": "ISO", "unreadCount": 3 }],
  "page": 1, "pageSize": 50, "total": 10 }
```

### GET /admin/chats/conversations/{userId}/messages
Query: `page`, `pageSize`
```json
// Response 200
{ "items": [{ "id": "uuid", "userId": "uuid", "managerId": "uuid",
              "senderType": "USER | MANAGER", "message": "...",
              "isRead": true, "createdAt": "ISO" }],
  "page": 1, "pageSize": 50, "total": 30 }
// Errors: 404 USER_NOT_FOUND
```

### POST /admin/chats/conversations/{userId}/messages
```json
// Request
{ "message": "Здравствуйте! Ваш заказ уже в филиале." }
// Response 201: ChatMessage (senderType: "MANAGER")
// Errors: 404 USER_NOT_FOUND, 400 VALIDATION_ERROR
```

### WebSocket — `ws://api.adesexpress.com/ws/chat` (cookie прикладывается автоматически)
- Подписка: `/user/queue/messages`
- Отправка: `/app/chat.send` → `{ "message": "текст" }`

---

## Отчёты — `/admin/reports`

### GET /admin/reports/users/summary (только SUPER_ADMIN)
Query: `period`, `from`, `to`
```json
// Response 200
{ "items": [{ "date": "2025-11-29", "newUsersCount": 12,
              "deletedUsersCount": 1, "totalUsersCount": 5000 }],
  "total": 31 }
// Errors: 403 FORBIDDEN, 400 VALIDATION_ERROR
```

### GET /admin/reports/users/summary/export (только SUPER_ADMIN)
Query: те же + `format` (xlsx|csv) → бинарный файл
