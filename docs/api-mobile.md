# Mobile API (Flutter)

Base URL: `https://api.adesexpress.com`

## Аутентификация
- Авторизованные запросы: `Authorization: Bearer <accessToken>`
- Без cookies

## Модели

### User
```json
{
  "id": "uuid",
  "role": "USER",
  "email": "user@example.com",
  "firstName": "Айдар",
  "lastName": "Тестов",
  "phone": "+996777000000",
  "personalCode": "AN0001",
  "branch": Branch,
  "status": 0,
  "createdAt": "ISO",
  "updatedAt": "ISO"
}
```

### Branch
```json
{
  "id": "uuid", "address": "г. Бишкек, Анкара-10", "personalCodePrefix": "AN",
  "latitude": 42.8746, "longitude": 74.5698,
  "photoUrl": "/uploads/branches/uuid_photo.jpg",
  "phone": "+996312000000", "workingHours": "Пн-Пт 09:00-18:00"
}
```
`latitude`, `longitude`, `photoUrl`, `phone`, `workingHours` — опциональны, могут быть `null`.

### Product
```json
{
  "id": "uuid",
  "hatch": "YT123456",
  "status": "IN_CHINA | ON_THE_WAY | IN_KG | DELIVERED",
  "price": 5000.00,
  "weight": 3.0,
  "createdAt": "ISO",
  "updatedAt": "ISO"
}
```
`price` и `weight` — опциональны, могут быть `null`.

### ItemsSummary
```json
{
  "productsByStatus": { "IN_CHINA": 2, "ON_THE_WAY": 5, "IN_KG": 1, "DELIVERED": 10 },
  "lastUpdatedAt": "ISO"
}
```

### News
```json
{ "id": "uuid", "image": "url", "title": "...", "content": "...", "createdAt": "ISO" }
```

### ChatMessage
```json
{
  "id": "uuid", "roomId": "uuid", "senderType": "USER | MANAGER",
  "senderId": "uuid", "senderName": "Айдар Тестов",
  "content": "...", "isRead": false, "createdAt": "ISO"
}
```

### ChatRoom
```json
{
  "id": "uuid", "userId": "uuid", "userFullName": "Айдар Тестов",
  "userPersonalCode": "AN0001", "branchId": "uuid", "branchAddress": "г. Бишкек, Анкара-10",
  "unreadCount": 3, "lastMessage": ChatMessage, "createdAt": "ISO"
}
```

---

## Auth — `/auth/*`

### POST /auth/register
```json
// Request
{ "phone": "+996777000000", "email": "user@example.com", "password": "StrongP@ssw0rd",
  "firstName": "Айдар", "lastName": "Тестов", "branchId": "uuid" }
// Response 201
{ "accessToken": "jwt", "refreshToken": "jwt", "user": User }
// Errors: 400 VALIDATION_ERROR, 409 CONFLICT
```

### POST /auth/login
Заголовок `X-Client-Type: mobile` обязателен.
```json
// Request
{ "login": "user@example.com", "password": "StrongP@ssw0rd" }
// Response 200
{ "accessToken": "jwt", "refreshToken": "jwt", "user": User }
// Errors: 400 VALIDATION_ERROR, 401 INVALID_CREDENTIALS, 403 FORBIDDEN
```

### POST /auth/refresh
```json
// Request
{ "refreshToken": "jwt" }
// Response 200
{ "accessToken": "jwt", "refreshToken": "jwt", "user": User }
// Errors: 400 VALIDATION_ERROR, 401 TOKEN_EXPIRED | INVALID_TOKEN
```

### POST /auth/logout
Требует `Authorization: Bearer`
```json
// Request
{ "refreshToken": "current-refresh-token" }
// Response 200
{ "success": true }
```

### POST /auth/confirm
```json
// Request
{ "login": "ivan_petrov", "code": "4821" }
// Response 204
// Errors: 404 USER_NOT_FOUND, 400 INVALID_CONFIRMATION_CODE
```

### POST /auth/resend
```
// Query: ?login=ivan_petrov
// Response 204
// Errors: 404 USER_NOT_FOUND, 400 INVALID_CONFIRMATION_CODE, 429 RESEND_TOO_SOON
```

### POST /auth/forgot-password
```json
// Request
{ "email": "ivan@example.com" }
// Response 204
// Errors: 404 USER_NOT_FOUND, 400 VALIDATION_ERROR
```

### POST /auth/forgot-password/verify
```json
// Request
{ "email": "ivan@example.com", "code": "4821" }
// Response 200
{ "resetToken": "uuid" }
// Errors: 404 USER_NOT_FOUND, 400 INVALID_RESET_CODE
```

### POST /auth/forgot-password/reset
```json
// Request
{ "resetToken": "uuid", "newPassword": "NewP@ssw0rd" }
// Response 204
// Errors: 400 INVALID_RESET_TOKEN | VALIDATION_ERROR
```

---

## Profile — `/users/me`

### GET /users/me → User

### PATCH /users/me
```json
// Request (все поля опциональные)
{ "firstName": "...", "lastName": "...", "email": "...", "phone": "..." }
// Response 200: User
// Errors: 400 VALIDATION_ERROR, 409 CONFLICT
```

### PATCH /users/me/change-password
```json
// Request
{ "currentPassword": "OldP@ssw0rd", "newPassword": "NewP@ssw0rd" }
// Response 200: { "success": true }
// Errors: 400 VALIDATION_ERROR, 401 INVALID_CREDENTIALS
```

### PATCH /users/me/branch
```json
// Request
{ "branchId": "uuid" }
// Response 200: User  (генерируется новый personalCode)
```

### POST /users/me/deletion-request
Без тела. Статус → PENDING_DELETION, инвалидирует все refresh кроме текущей.
```json
// Response 201
{ "success": true, "deletionDate": "2026-03-22T00:00:00Z" }
// Errors: 409 CONFLICT (уже помечен)
```

### DELETE /users/me/deletion-request
Без тела. Статус → ACTIVE.
```json
// Response 200: { "success": true }
// Errors: 409 CONFLICT (не помечен)
```

---

## Items & Products

### GET /items/summary → ItemsSummary

### GET /products/my
Query: `status`, `page`, `pageSize`
```json
// Response 200
{ "items": [Product], "page": 1, "pageSize": 20, "total": 30 }
```

### GET /products/{productId} → Product
Errors: 404 PRODUCT_NOT_FOUND

### GET /products/{productId}/history
```json
// Response 200
{ "items": [{ "id": "uuid", "status": "IN_CHINA", "createdAt": "ISO" }], "total": 3 }
```

---

## News

### GET /news (без авторизации)
Query: `page`, `pageSize`
```json
// Response 200
{ "items": [News], "page": 1, "pageSize": 20, "total": 10 }
```

### GET /news/{newsId} → News
Errors: 404 NEWS_NOT_FOUND

---

## Chat

### GET /chat/rooms — Список чатов
```json
// Response 200
[ChatRoom]
```

### GET /chat/rooms/{roomId}/messages — История сообщений
Query: `page`, `pageSize`
```json
// Response 200
{ "items": [ChatMessage], "page": 1, "pageSize": 20, "total": 45 }
// Errors: 403 FORBIDDEN, 404 CHAT_ROOM_NOT_FOUND
```

### POST /chat/rooms/{roomId}/messages/read — Пометить как прочитанные
Response 200. Errors: 403 FORBIDDEN, 404 CHAT_ROOM_NOT_FOUND

### GET /chat/unread-count — Счётчик непрочитанных
```json
// Response 200
{ "totalUnread": 5 }
```

### WebSocket — `ws://api.adesexpress.com/ws`
STOMP CONNECT header: `Authorization: Bearer <accessToken>`
- Подписка: `/topic/chat.room.{roomId}`
- Отправка: `/app/chat.send` → `{ "roomId": "uuid", "content": "текст" }`
- Прочитано: `/app/chat.read` → `{ "roomId": "uuid" }`
- Ошибки: `/user/queue/errors`
- Получение: ChatMessage

---

## Push-токены

### POST /push-tokens
```json
// Request
{ "token": "ExponentPushToken[xxx]" }
// Response 200: { "success": true }
// Errors: 400 VALIDATION_ERROR
```

---

## Branches

### GET /branches (без авторизации)
```json
// Response 200
{ "items": [Branch], "total": 5 }
```
