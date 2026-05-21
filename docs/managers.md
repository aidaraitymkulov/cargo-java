# Менеджеры (Managers)

## Обзор

Менеджеры — сотрудники административной панели (React). Это отдельная сущность от `users` — разные таблицы, разные сессии, разный механизм аутентификации.

**Роли:**
| Роль | Кто | Доступ |
|---|---|---|
| `MANAGER` | Сотрудник филиала | Данные только своего филиала |
| `SUPER_ADMIN` | Главный администратор | Полный доступ ко всему |

Менеджеры не регистрируются сами — создаются только через `SUPER_ADMIN`. Вход — через `/auth/login` с заголовком `X-Client-Type: web` (токены в HttpOnly cookie, см. `auth.md`).

---

## Модель данных

```json
{
  "id": "uuid",
  "login": "manager_bishkek",
  "password": "plain_text_password",
  "firstName": "Алия",
  "lastName": "Сатыбалдиева",
  "phone": "+996700123456",
  "role": "MANAGER",
  "branch": {
    "id": "uuid",
    "address": "ул. Киевская 77",
    "personalCodePrefix": "AN",
    "isActive": true
  },
  "createdAt": "2024-01-15T10:30:00"
}
```

> **Важно:** поле `password` содержит пароль в открытом виде — намеренно, для отображения в React-панели. В БД пароль хранится и в виде хэша (`password_hash`), и в открытом виде (`password`).

---

## Admin API — собственный профиль

### GET /admin/me
Профиль текущего менеджера.

**Auth:** `MANAGER` или `SUPER_ADMIN` (cookie)

**Response `200 OK`:** `ManagerResponse`

---

## Admin API — управление менеджерами

Все эндпоинты требуют роль `SUPER_ADMIN`.

### POST /admin/managers
Создание нового менеджера.

**Request:**
```json
{
  "login": "manager_osh",
  "password": "secret123",
  "firstName": "Бекзат",
  "lastName": "Усупов",
  "phone": "+996700555444",
  "branchId": "uuid"
}
```

**Валидация:**
- `login` — обязателен, минимум 3 символа, уникальный
- `password` — обязателен, минимум 6 символов
- `firstName`, `lastName`, `phone` — обязательны
- `branchId` — обязателен, существующий филиал

**Response `201 Created`:** `ManagerResponse`

**Ошибки:**
```
409 CONFLICT         — логин уже занят
404 BRANCH_NOT_FOUND — филиал не найден
400 VALIDATION_ERROR — нарушена валидация полей
```

Новый менеджер всегда создаётся с ролью `MANAGER`. Роль `SUPER_ADMIN` назначается вручную в БД.

---

### GET /admin/managers
Список всех менеджеров. `SUPER_ADMIN` в список не включается.

**Response `200 OK`:** массив `ManagerResponse`, отсортированный по `createdAt` DESC

---

### GET /admin/managers/{managerId}

**Response `200 OK`:** `ManagerResponse`

**Ошибки:**
```
404 NOT_FOUND — менеджер не найден
```

---

### PATCH /admin/managers/{managerId}
Обновление данных менеджера. Все поля опциональны, `null` игнорируется.

**Нельзя редактировать `SUPER_ADMIN`** — вернёт `403 FORBIDDEN`.

**Request:**
```json
{
  "login": "new_login",
  "password": "new_password",
  "firstName": "Новое",
  "lastName": "Имя",
  "phone": "+996700000001",
  "branchId": "uuid"
}
```

**Валидация:**
- `login` — минимум 3 символа (если передан)
- `password` — минимум 6 символов (если передан)
- `firstName`, `lastName` — максимум 100 символов
- `phone` — максимум 20 символов

**Response `200 OK`:** `ManagerResponse`

**Ошибки:**
```
404 NOT_FOUND        — менеджер не найден
403 FORBIDDEN        — попытка редактировать SUPER_ADMIN
409 CONFLICT         — новый логин уже занят
404 BRANCH_NOT_FOUND — новый филиал не найден
400 VALIDATION_ERROR — нарушена валидация
```

---

### DELETE /admin/managers/{managerId}
Удаление менеджера.

**Ограничения:**
- Нельзя удалить самого себя
- Нельзя удалить `SUPER_ADMIN`

**Response `204 No Content`**

**Ошибки:**
```
404 NOT_FOUND — менеджер не найден
403 FORBIDDEN — попытка удалить SUPER_ADMIN
409 CONFLICT  — попытка удалить себя
```
