# Филиалы (Branches)

## Обзор

Филиал — это физическое отделение в Кыргызстане, к которому привязан клиент при регистрации. Каждый филиал имеет уникальный `personalCodePrefix`, из которого генерируются персональные коды клиентов (`AN0001`, `D0015` и т.д.).

**Роли:**
- `SUPER_ADMIN` — полный CRUD через Admin API
- `USER` — только просмотр активных филиалов через Mobile API (для выбора при регистрации)
- `MANAGER` — не имеет прямого доступа к управлению филиалами

---

## Модель данных

```json
{
  "id": "uuid",
  "address": "ул. Киевская 77",
  "personalCodePrefix": "AN",
  "isActive": true
}
```

| Поле | Описание |
|---|---|
| `id` | UUID филиала |
| `address` | Адрес отделения |
| `personalCodePrefix` | Уникальный префикс для кодов клиентов (`AN`, `D`, `OW`) |
| `isActive` | Активен ли филиал (отображается клиентам при регистрации) |

---

## Mobile API

### GET /branches
Список активных филиалов. Используется Flutter-клиентом при регистрации.

**Auth:** не требуется (публичный эндпоинт)

**Response `200 OK`:**
```json
[
  {
    "id": "uuid",
    "address": "ул. Киевская 77",
    "personalCodePrefix": "AN",
    "isActive": true
  }
]
```

Возвращает только филиалы с `isActive = true`. Деактивированные филиалы не отображаются.

---

## Admin API

Все эндпоинты требуют роль `SUPER_ADMIN`.

### GET /admin/branches
Список всех филиалов, включая деактивированные.

**Response `200 OK`:** массив `BranchResponse`

---

### GET /admin/branches/{id}

**Response `200 OK`:** один `BranchResponse`

**Ошибки:**
```
404 BRANCH_NOT_FOUND — филиал не найден
```

---

### POST /admin/branches

**Request:**
```json
{
  "address": "ул. Манаса 42",
  "personalCodePrefix": "MN"
}
```

**Валидация:**
- `address` — обязателен, не пустой
- `personalCodePrefix` — обязателен, уникальный в системе

**Response `201 Created`:** `BranchResponse`

**Ошибки:**
```
409 CONFLICT         — префикс уже занят другим филиалом
400 VALIDATION_ERROR — нарушена валидация полей
```

**Важно:** новый филиал создаётся с `isActive = true` и `nextSequence = 1`.

---

### PATCH /admin/branches/{id}
Обновление адреса. Префикс (`personalCodePrefix`) не меняется после создания.

**Request:**
```json
{
  "address": "ул. Манаса 100"
}
```

Все поля опциональны. `null`-поля игнорируются.

**Response `200 OK`:** `BranchResponse`

**Ошибки:**
```
404 BRANCH_NOT_FOUND — филиал не найден
```

---

### PATCH /admin/branches/{id}/activate
Активировать филиал (`isActive = true`). После активации филиал снова появляется в Mobile API.

**Response `200 OK`:** `BranchResponse`

**Ошибки:**
```
404 BRANCH_NOT_FOUND — филиал не найден
```

---

### PATCH /admin/branches/{id}/deactivate
Деактивировать филиал (`isActive = false`). Филиал скрывается из Mobile API.

**Response `200 OK`:** `BranchResponse`

**Ошибки:**
```
404 BRANCH_NOT_FOUND — филиал не найден
```

---

## PersonalCode — генерация

При регистрации клиента к выбранному филиалу:

1. Берётся `personalCodePrefix` филиала (например, `AN`)
2. Берётся текущий `nextSequence` (например, `15`)
3. Формат: `prefix + номер с ведущими нулями до 4 цифр` → `AN0015`
4. `nextSequence` инкрементируется и сохраняется

Строка филиала блокируется (`SELECT ... FOR UPDATE`) на время генерации, чтобы исключить дубликаты при параллельных регистрациях.

**Примеры кодов:** `AN0001`, `D0233`, `OW0021`

При смене клиентом филиала генерируется новый personalCode с префиксом нового филиала. Старый код остаётся валидным (история хранится в `user_personal_codes`).
