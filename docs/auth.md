# Аутентификация

## Обзор

В системе два типа клиентов с разными механизмами аутентификации:

| | Mobile (Flutter) | Web (React Admin) |
|---|---|---|
| Заголовок | `X-Client-Type: mobile` | `X-Client-Type: web` |
| Хранение токенов | В теле ответа | HttpOnly cookie |
| Тип пользователя | `USER` | `MANAGER` / `SUPER_ADMIN` |
| Таблица сессий | `refresh_sessions` | `manager_refresh_sessions` |
| Защищённые запросы | `Authorization: Bearer <token>` | Cookie автоматически |

Оба клиента используют одни и те же эндпоинты `/auth/*`. Поведение определяется заголовком `X-Client-Type`.

---

## JWT токены

- **Access token** — 15 минут, содержит `userId` (или `managerId`) и `jti`
- **Refresh token** — 30 дней, содержит только `jti`

Каждая refresh-сессия хранится в БД. При logout — `revoked_at` проставляется немедленно. При refresh — старая сессия отзывается, создаётся новая (rotation).

---

## Эндпоинты

### POST /auth/register
Только для мобильных пользователей (Flutter). Менеджеров создаёт SUPER_ADMIN через `/admin/managers`.

**Request:**
```json
{
  "login": "ivan_petrov",
  "firstName": "Иван",
  "lastName": "Петров",
  "email": "ivan@example.com",
  "phone": "+996700123456",
  "dateOfBirth": "1995-03-15",
  "password": "secret123",
  "branchId": "uuid"
}
```

**Валидация:**
- `login` — минимум 3 символа, уникальный
- `email` — валидный формат, уникальный
- `password` — минимум 6 символов
- `branchId` — существующий филиал

**Response:** `201 Created` (пустое тело)

**Побочные эффекты:**
- Генерируется `personalCode` по формату `{prefix}{номер}` (например, `AN0001`)
- Создаётся запись в `user_personal_codes`
- Инкрементируется `branches.next_sequence`
- На email отправляется 4-значный код подтверждения (TTL 5 минут)

**Ошибки:**
```
409 CONFLICT          — логин или email уже заняты
404 BRANCH_NOT_FOUND  — филиал не найден
400 VALIDATION_ERROR  — нарушена валидация полей
```

---

### POST /auth/confirm
Подтверждение email после регистрации. Только для мобильных пользователей.

**Request:**
```json
{
  "login": "ivan_petrov",
  "code": "4821"
}
```

**Response:** `204 No Content`

**Логика:**
- Код живёт 5 минут
- 3 попытки, при исчерпании — код становится недействительным
- При неверном коде — попытка списывается, ошибка `INVALID_CONFIRMATION_CODE`

**Ошибки:**
```
404 USER_NOT_FOUND            — пользователь не найден
400 INVALID_CONFIRMATION_CODE — нет активного кода / код истёк / неверный код / попытки исчерпаны
```

---

### POST /auth/resend
Повторная отправка кода подтверждения. Только для мобильных пользователей.

**Query params:** `?login=ivan_petrov`

**Response:** `204 No Content`

**Ограничение:** повторная отправка не чаще 1 раза в 60 секунд.

**Ошибки:**
```
404 USER_NOT_FOUND            — пользователь не найден
400 INVALID_CONFIRMATION_CODE — нет активного кода подтверждения
429 RESEND_TOO_SOON           — прошло меньше 60 секунд с прошлой отправки
```

---

### POST /auth/login
Единый эндпоинт. Поведение определяется заголовком `X-Client-Type`.

**Request (оба клиента):**
```json
{
  "login": "ivan_petrov",
  "password": "secret123"
}
```

#### Mobile (`X-Client-Type: mobile`)

Ищет пользователя в таблице `users`. Токены возвращаются в теле.

**Response `200 OK`:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "user": {
    "id": "uuid",
    "email": "ivan@example.com",
    "firstName": "Иван",
    "lastName": "Петров",
    "phone": "+996700123456",
    "dateOfBirth": "1995-03-15",
    "personalCode": "AN0001",
    "branch": {
      "id": "uuid",
      "address": "ул. Киевская 77"
    },
    "status": "ACTIVE",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**Блокирующие условия:**
- Email не подтверждён → `403 EMAIL_NOT_CONFIRMED`
- Статус `INACTIVE` → `403 FORBIDDEN`
- Статус `DELETED` или `PENDING_DELETION` → `401 INVALID_CREDENTIALS` (намеренно скрываем факт существования)

#### Web (`X-Client-Type: web`)

Ищет менеджера в таблице `managers`. Токены устанавливаются в `HttpOnly` cookies.

**Response `200 OK`** (тело — только данные менеджера, без токенов):
```json
{
  "id": "uuid",
  "login": "manager1",
  "firstName": "Алия",
  "lastName": "Сатыбалдиева",
  "role": "MANAGER",
  "branch": {
    "id": "uuid",
    "address": "ул. Киевская 77"
  }
}
```

**Set-Cookie (2 штуки):**
```
Set-Cookie: accessToken=eyJ...; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=900
Set-Cookie: refreshToken=eyJ...; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=2592000
```

**Ошибки (оба клиента):**
```
401 INVALID_CREDENTIALS — неверный логин или пароль
403 FORBIDDEN           — аккаунт заблокирован (только mobile)
403 EMAIL_NOT_CONFIRMED — email не подтверждён (только mobile)
```

---

### POST /auth/refresh
Обновление токенов. Старая сессия отзывается, выдаётся новая пара токенов (rotation).

#### Mobile (`X-Client-Type: mobile`)

**Request:**
```json
{
  "refreshToken": "eyJ..."
}
```

**Response `200 OK`:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ..."
}
```

#### Web (`X-Client-Type: web`)

Refresh token берётся из cookie автоматически. Новые токены устанавливаются в cookie.

**Response:** `204 No Content`

**Ошибки:**
```
401 INVALID_TOKEN  — токен невалиден, истёк или сессия отозвана
400 VALIDATION_ERROR — refreshToken отсутствует в теле (только mobile)
```

---

### POST /auth/logout
Отзывает refresh-сессию.

#### Mobile (`X-Client-Type: mobile`)

**Request:**
```json
{
  "refreshToken": "eyJ..."
}
```

#### Web (`X-Client-Type: web`)

Refresh token берётся из cookie. После logout cookies очищаются (устанавливаются с `Max-Age=0`).

**Response:** `204 No Content` (оба клиента)

**Ошибки:**
```
401 INVALID_TOKEN    — токен невалиден
400 VALIDATION_ERROR — refreshToken отсутствует в теле (только mobile)
```

---

## Флоу регистрации (Mobile)

```
1. POST /auth/register  →  201, письмо с кодом на email
2. POST /auth/confirm   →  204, email подтверждён
3. POST /auth/login     →  200, accessToken + refreshToken в теле
```

## Флоу входа и обновления токена (Mobile)

```
1. POST /auth/login    →  accessToken (15 мин) + refreshToken (30 дней)
2. ...accessToken истёк...
3. POST /auth/refresh  →  новый accessToken + новый refreshToken
4. POST /auth/logout   →  refresh-сессия отозвана
```

## Флоу входа (Web)

```
1. POST /auth/login (X-Client-Type: web)  →  200, токены в HttpOnly cookie
2. Все запросы к /admin/* — cookie отправляется браузером автоматически
3. POST /auth/refresh (X-Client-Type: web) →  204, cookie обновлены
4. POST /auth/logout  (X-Client-Type: web) →  204, cookie очищены
```

---

## Защита эндпоинтов

Для Mobile API в заголовке запроса:
```
Authorization: Bearer <accessToken>
```

Для Admin API cookie отправляется браузером автоматически при `credentials: 'include'`.

`JwtAuthFilter` автоматически определяет тип клиента:
- Есть заголовок `Authorization: Bearer` → мобильный, ищет сессию в `refresh_sessions`
- Есть cookie `accessToken` → веб, ищет сессию в `manager_refresh_sessions`
