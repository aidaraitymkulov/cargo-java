# Домен: Чат (Chat)

Чат между мобильными пользователями (Flutter) и менеджерами (React-панель). WebSocket + STOMP для realtime, REST API для истории и списков.

---

## Концепция

- У каждого пользователя может быть **несколько чатов** — по одному на каждый филиал
- При смене филиала старый чат сохраняется, новый создаётся при первом сообщении
- **MANAGER** видит только чаты своего филиала
- **SUPER_ADMIN** видит и отвечает во всех чатах
- Пользователь с `chatBanned = true` не может отправлять сообщения

---

## Модели

### ChatRoomResponse

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "userId": "a1b2c3d4-...",
  "userFullName": "Айдар Тестов",
  "userPersonalCode": "AN0001",
  "branchId": "b5c6d7e8-...",
  "branchAddress": "г. Бишкек, Анкара-10",
  "unreadCount": 3,
  "lastMessage": ChatMessageResponse,
  "createdAt": "2026-06-01T10:00:00"
}
```

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | ID комнаты |
| `userId` | UUID | ID пользователя |
| `userFullName` | string | Имя + фамилия пользователя |
| `userPersonalCode` | string | Персональный код (AN0001) |
| `branchId` | UUID | ID филиала |
| `branchAddress` | string | Адрес филиала |
| `unreadCount` | long | Количество непрочитанных сообщений |
| `lastMessage` | ChatMessageResponse? | Последнее сообщение (null если чат пустой) |
| `createdAt` | ISO 8601 datetime | Дата создания комнаты |

### ChatMessageResponse

```json
{
  "id": "e9f0a1b2-...",
  "roomId": "3fa85f64-...",
  "senderType": "USER",
  "senderId": "a1b2c3d4-...",
  "senderName": "Айдар Тестов",
  "content": "Здравствуйте, где мой заказ?",
  "isRead": false,
  "createdAt": "2026-06-01T10:30:00"
}
```

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | ID сообщения |
| `roomId` | UUID | ID комнаты |
| `senderType` | string | `USER` или `MANAGER` |
| `senderId` | UUID | ID отправителя |
| `senderName` | string | Имя отправителя |
| `content` | string | Текст сообщения (макс. 2000 символов) |
| `isRead` | boolean | Прочитано получателем |
| `createdAt` | ISO 8601 datetime | Дата отправки |

### UnreadCountResponse

```json
{
  "totalUnread": 5
}
```

---

## Mobile API — `/chat`

Требуется авторизация через `Authorization: Bearer <accessToken>`.

---

### GET /chat/rooms — Список чатов пользователя

Возвращает все чат-комнаты пользователя (по одной на филиал).

**Ответ 200**

```json
[
  {
    "id": "3fa85f64-...",
    "userId": "a1b2c3d4-...",
    "userFullName": "Айдар Тестов",
    "userPersonalCode": "AN0001",
    "branchId": "b5c6d7e8-...",
    "branchAddress": "г. Бишкек, Анкара-10",
    "unreadCount": 2,
    "lastMessage": {
      "id": "e9f0a1b2-...",
      "roomId": "3fa85f64-...",
      "senderType": "MANAGER",
      "senderId": "f1g2h3i4-...",
      "senderName": "Айгуль Менеджер",
      "content": "Ваш заказ прибыл",
      "isRead": false,
      "createdAt": "2026-06-01T14:00:00"
    },
    "createdAt": "2026-06-01T10:00:00"
  }
]
```

**Ошибки**

| Код | error | Причина |
|-----|-------|---------|
| 401 | `UNAUTHORIZED` | Нет токена |

---

### GET /chat/rooms/{roomId}/messages — История сообщений

**Path-параметры**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `roomId` | UUID | ID комнаты |

**Query-параметры**

| Параметр | Тип | По умолчанию | Ограничение |
|----------|-----|--------------|-------------|
| `page` | int | `1` | ≥ 1 |
| `pageSize` | int | `20` | ≥ 1 |

**Ответ 200**

```json
{
  "items": [ChatMessageResponse],
  "page": 1,
  "pageSize": 20,
  "total": 45
}
```

Сортировка: по `createdAt` по убыванию (новые первыми).

**Ошибки**

| Код | error | Причина |
|-----|-------|---------|
| 401 | `UNAUTHORIZED` | — |
| 403 | `FORBIDDEN` | Чат принадлежит другому пользователю |
| 404 | `CHAT_ROOM_NOT_FOUND` | Комната не найдена |

---

### POST /chat/rooms/{roomId}/messages/read — Пометить как прочитанные

Помечает все сообщения от менеджера в данной комнате как прочитанные.

**Path-параметры**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `roomId` | UUID | ID комнаты |

**Ответ 200** — тело отсутствует

**Ошибки**

| Код | error | Причина |
|-----|-------|---------|
| 401 | `UNAUTHORIZED` | — |
| 403 | `FORBIDDEN` | Чат принадлежит другому пользователю |
| 404 | `CHAT_ROOM_NOT_FOUND` | Комната не найдена |

---

### GET /chat/unread-count — Счётчик непрочитанных

Общее количество непрочитанных сообщений от менеджеров по всем чатам пользователя.

**Ответ 200** — `UnreadCountResponse`

---

## Admin API — `/admin/chat`

Требуется авторизация через HttpOnly cookie. Роли: `MANAGER`, `SUPER_ADMIN`.

---

### GET /admin/chat/rooms — Список чатов

- **MANAGER** — чаты пользователей своего филиала
- **SUPER_ADMIN** — все чаты

**Ответ 200** — `[ChatRoomResponse]`

`unreadCount` считает непрочитанные сообщения от пользователей (USER).

---

### GET /admin/chat/rooms/{roomId}/messages — История сообщений

Аналогично Mobile API, но с проверкой доступа менеджера к филиалу комнаты.

**Query-параметры**

| Параметр | Тип | По умолчанию | Ограничение |
|----------|-----|--------------|-------------|
| `page` | int | `1` | ≥ 1 |
| `pageSize` | int | `20` | ≥ 1 |

**Ответ 200** — `PagedResponse<ChatMessageResponse>`

**Ошибки**

| Код | error | Причина |
|-----|-------|---------|
| 401 | `UNAUTHORIZED` | — |
| 403 | `FORBIDDEN` | Менеджер не имеет доступа к филиалу комнаты |
| 404 | `CHAT_ROOM_NOT_FOUND` | — |

---

### POST /admin/chat/rooms/{roomId}/messages/read — Пометить как прочитанные

Помечает все сообщения от пользователя в данной комнате как прочитанные.

**Ответ 200** — тело отсутствует

---

### GET /admin/chat/unread-count — Счётчик непрочитанных

- **MANAGER** — непрочитанные сообщения от пользователей в чатах своего филиала
- **SUPER_ADMIN** — непрочитанные сообщения от пользователей по всем филиалам

**Ответ 200** — `UnreadCountResponse`

---

## WebSocket (STOMP)

### Подключение

**Endpoint:** `ws://localhost:8080/ws`

**Аутентификация при CONNECT:**
- **Flutter (mobile):** передать STOMP header `Authorization: Bearer <accessToken>`
- **React (admin):** cookie `accessToken` извлекается автоматически из HTTP upgrade запроса

### Топики

| Направление | Destination | Описание |
|-------------|-------------|----------|
| Клиент → сервер | `/app/chat.send` | Отправить сообщение |
| Клиент → сервер | `/app/chat.read` | Пометить как прочитанное |
| Сервер → клиент | `/topic/chat.room.{roomId}` | Новое сообщение в комнате |
| Сервер → клиент | `/user/queue/errors` | Ошибки (персональный канал) |

### Отправка сообщения

Destination: `/app/chat.send`

```json
{
  "roomId": "3fa85f64-...",
  "content": "Здравствуйте, где мой заказ?"
}
```

Сервер отправляет `ChatMessageResponse` в `/topic/chat.room.{roomId}`.

### Пометить как прочитанное

Destination: `/app/chat.read`

```json
{
  "roomId": "3fa85f64-..."
}
```

### Ошибки WebSocket

Ошибки приходят в `/user/queue/errors`:

```json
{
  "error": "CHAT_BANNED",
  "message": "Вы заблокированы в чате"
}
```

---

## Коды ошибок

| error | HTTP | Описание |
|-------|------|----------|
| `CHAT_ROOM_NOT_FOUND` | 404 | Комната не найдена |
| `CHAT_BANNED` | 403 | Пользователь заблокирован в чате |
| `FORBIDDEN` | 403 | Нет доступа к чату |
| `USER_NOT_FOUND` | 404 | Пользователь не найден |
| `MANAGER_NOT_FOUND` | 404 | Менеджер не найден |
| `UNAUTHORIZED` | 401 | Нет токена авторизации |
| `VALIDATION_ERROR` | 400 | Ошибка валидации полей |