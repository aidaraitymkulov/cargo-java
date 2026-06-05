# Пользователи (Users)

## Обзор

Пользователи — мобильные клиенты системы (роль `USER`). Управляют своим профилем через Mobile API. Администраторы просматривают и модерируют пользователей через Admin API.

Менеджеры (`MANAGER`, `SUPER_ADMIN`) — отдельная сущность, описана в `managers.md`.

---

## Модель данных

```json
{
  "id": "uuid",
  "email": "ivan@example.com",
  "firstName": "Иван",
  "lastName": "Петров",
  "phone": "+996700123456",
  "dateOfBirth": "1995-03-15",
  "personalCode": "AN0001",
  "branch": {
    "id": "uuid",
    "address": "ул. Киевская 77",
    "personalCodePrefix": "AN",
    "isActive": true
  },
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Статусы пользователя

| Статус | Описание |
|---|---|
| `ACTIVE` | Нормальная работа |
| `INACTIVE` | Заблокирован администратором, вход запрещён |
| `PENDING_DELETION` | Пользователь запросил удаление, аккаунт анонимизируется через 30 дней |
| `DELETED` | Жёстко удалён администратором, данные анонимизированы |

---

## Mobile API

Все эндпоинты требуют `Authorization: Bearer <accessToken>`.

### GET /users/me
Профиль текущего пользователя.

**Response `200 OK`:** `UserResponse`

**Ошибки:**
```
404 USER_NOT_FOUND — пользователь не найден
```

---

### PATCH /users/me
Обновление профиля. Все поля опциональны, `null`-поля игнорируются.

**Request:**
```json
{
  "firstName": "Иван",
  "lastName": "Петров",
  "email": "new@example.com",
  "phone": "+996700999888"
}
```

**Валидация:**
- `firstName`, `lastName` — максимум 100 символов
- `email` — валидный формат, уникальный в системе
- `phone` — максимум 20 символов

**Response `200 OK`:** `UserResponse`

**Ошибки:**
```
409 CONFLICT         — новый email уже занят другим пользователем
400 VALIDATION_ERROR — нарушена валидация полей
```

---

### PATCH /users/me/change-password

**Request:**
```json
{
  "currentPassword": "old_secret",
  "newPassword": "new_secret123"
}
```

**Валидация:**
- `currentPassword` — обязателен
- `newPassword` — обязателен, минимум 8 символов

**Response `200 OK`:**
```json
{ "success": true }
```

**Ошибки:**
```
401 INVALID_CREDENTIALS — текущий пароль неверен
400 VALIDATION_ERROR    — нарушена валидация
```

---

### PATCH /users/me/branch
Смена филиала. Генерирует новый `personalCode` с префиксом нового филиала. Старый personalCode деактивируется, но остаётся в истории.

**Request:**
```json
{
  "branchId": "uuid"
}
```

**Response `200 OK`:** `UserResponse` с новым `personalCode` и `branch`

**Побочные эффекты:**
- Все активные записи в `user_personal_codes` деактивируются
- Создаётся новая запись с новым personalCode
- `branches.next_sequence` инкрементируется
- Строка филиала блокируется на время генерации кода

**Ошибки:**
```
404 BRANCH_NOT_FOUND — филиал не найден
```

---

### POST /users/me/deletion-request
Запрос на удаление аккаунта. Переводит статус в `PENDING_DELETION` и отзывает все активные сессии.

**Response `201 Created`:**
```json
{
  "success": true,
  "deletionDate": "2024-02-14T10:30:00"
}
```

`deletionDate` — дата фактической анонимизации (30 дней от момента запроса).

**Побочные эффекты:**
- Все активные refresh-сессии немедленно отзываются
- Пользователь разлогинивается на всех устройствах
- Фоновый cron анонимизирует данные через 30 дней

**Ошибки:**
```
409 CONFLICT — аккаунт уже в статусе PENDING_DELETION
```

---

### DELETE /users/me/deletion-request
Отмена запроса на удаление. Переводит статус обратно в `ACTIVE`.

**Response `200 OK`:**
```json
{ "success": true }
```

**Ошибки:**
```
409 CONFLICT — аккаунт не находится в статусе PENDING_DELETION
```

---

## Admin API

Все эндпоинты требуют роль `MANAGER` или `SUPER_ADMIN`.

### GET /admin/users
Список пользователей с фильтрацией и пагинацией.

**Query параметры:**

| Параметр | Тип | Описание |
|---|---|---|
| `prefix` | string | Фильтр по префиксу personalCode (например, `AN`) |
| `code` | string | Фильтр по полному personalCode (например, `AN0001`) |
| `branchId` | uuid | Фильтр по филиалу |
| `page` | int | Номер страницы, default `1` |
| `pageSize` | int | Размер страницы, default `20` |

**Ограничение по роли:** `MANAGER` автоматически видит только пользователей своего филиала — `branchId` из запроса игнорируется и подставляется филиал менеджера. `SUPER_ADMIN` видит всех, может фильтровать по любому `branchId`.

**Response `200 OK`:**
```json
{
  "items": [ UserResponse ],
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

---

### GET /admin/users/search
Поиск пользователей по имени, фамилии или personalCode.

**Query параметры:**

| Параметр | Тип | Описание |
|---|---|---|
| `q` | string | Строка поиска (имя, фамилия или personalCode) |
| `page` | int | Номер страницы, default `1` |
| `pageSize` | int | Размер страницы, default `20` |

**Ограничение по роли:** `MANAGER` видит только пользователей своего филиала. `SUPER_ADMIN` ищет по всем филиалам.

**Response `200 OK`:**
```json
{
  "items": [ UserResponse ],
  "page": 1,
  "pageSize": 20,
  "total": 5
}
```

---

### GET /admin/users/stats
Статистика пользователей.

**Query параметры:**

| Параметр | Тип | Описание |
|---|---|---|
| `branchId` | uuid | Опционально. Фильтр по филиалу (только для SUPER_ADMIN) |

**Ограничение по роли:** `MANAGER` всегда получает статистику своего филиала.

**Response `200 OK`:**
```json
{
  "total": 450,
  "newThisMonth": 32
}
```

`newThisMonth` — пользователи, зарегистрированные в текущем календарном месяце.

---

### GET /admin/users/{userId}
Данные конкретного пользователя.

**Response `200 OK`:** `UserResponse`

**Ошибки:**
```
404 USER_NOT_FOUND — пользователь не найден
```

---

### DELETE /admin/users/delete/{userId}
Жёсткое удаление пользователя администратором. Устанавливает статус `DELETED` (без ожидания 30 дней).

**Response `204 No Content`**

**Ошибки:**
```
404 USER_NOT_FOUND — пользователь не найден
```

---

### PATCH /admin/users/{userId}/chat-ban
Запрет отправки сообщений в чат.

**Response `200 OK`:** `UserResponse` с `chatBanned: true`

**Ошибки:**
```
404 USER_NOT_FOUND — пользователь не найден
```

---

### PATCH /admin/users/{userId}/chat-unban
Снятие запрета чата.

**Response `200 OK`:** `UserResponse` с `chatBanned: false`

**Ошибки:**
```
404 USER_NOT_FOUND — пользователь не найден
```

---

## Фоновая анонимизация (Cron)

Ежедневно запускается задача, которая находит пользователей со статусом `PENDING_DELETION`, у которых прошло более 30 дней с момента запроса, и анонимизирует их данные:

- `email` → случайная строка
- `firstName`, `lastName`, `phone` → обезличенные значения
- Статус → `DELETED`
