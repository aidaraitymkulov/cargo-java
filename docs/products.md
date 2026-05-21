# Товары (Products)

## Обзор

Товар (product) — одна физическая посылка клиента. Имеет трекинг-номер (`hatch`) и проходит через статусы от Китая до выдачи. Товары создаются **только через импорт Excel** — нет API для ручного создания.

Вес и цена у товара отсутствуют — они принадлежат заказу (`Order`), который появляется при импорте из КР.

---

## Статусы товара

```
IN_CHINA → ON_THE_WAY → IN_KG → DELIVERED
```

| Статус | Описание |
|---|---|
| `IN_CHINA` | Товар зарегистрирован в Китае |
| `ON_THE_WAY` | В пути (авто-переход через 24ч или вручную через импорт) |
| `IN_KG` | Прибыл в КР, создан заказ |
| `DELIVERED` | Выдан клиенту |

**Фоновый cron:** товары со статусом `IN_CHINA` старше 24 часов автоматически переводятся в `ON_THE_WAY`.

**Важно:** дубликаты `hatch` — это норма. Один и тот же трекинг-номер может встречаться у одного клиента несколько раз (разные отправки).

---

## Модели данных

### ProductResponse (Mobile)
```json
{
  "id": "uuid",
  "hatch": "YT123456789",
  "status": "IN_CHINA",
  "orderId": null,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

`orderId` — `null` пока товар не привязан к заказу (статус `IN_CHINA` или `ON_THE_WAY`).

### ProductAdminResponse (Admin)
```json
{
  "id": "uuid",
  "hatch": "YT123456789",
  "userId": "uuid",
  "firstName": "Иван",
  "lastName": "Петров",
  "personalCode": "AN0001",
  "orderId": "uuid",
  "status": "IN_KG",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-16T08:00:00"
}
```

---

## Mobile API

Все эндпоинты требуют `Authorization: Bearer <accessToken>`.

### GET /products/my
Список товаров текущего пользователя с пагинацией и фильтром по статусу.

**Query параметры:**

| Параметр | Тип | Описание |
|---|---|---|
| `status` | string | Опционально. Один из: `IN_CHINA`, `ON_THE_WAY`, `IN_KG`, `DELIVERED` |
| `page` | int | Default `1` |
| `pageSize` | int | Default `20` |

**Response `200 OK`:**
```json
{
  "items": [ ProductResponse ],
  "page": 1,
  "pageSize": 20,
  "total": 45
}
```

Сортировка: `createdAt` DESC.

**Ошибки:**
```
400 VALIDATION_ERROR — передан неизвестный статус
```

---

### GET /products/{productId}
Конкретный товар. Возвращает только если товар принадлежит текущему пользователю.

**Response `200 OK`:** `ProductResponse`

**Ошибки:**
```
404 PRODUCT_NOT_FOUND — товар не найден или не принадлежит пользователю
```

---

### GET /products/{productId}/history
История изменений статуса товара. Возвращает только если товар принадлежит текущему пользователю.

**Response `200 OK`:**
```json
{
  "items": [
    {
      "id": "uuid",
      "status": "IN_CHINA",
      "createdAt": "2024-01-15T10:30:00"
    },
    {
      "id": "uuid",
      "status": "ON_THE_WAY",
      "createdAt": "2024-01-16T08:00:00"
    }
  ],
  "total": 2
}
```

Сортировка: `createdAt` ASC (хронологический порядок).

**Ошибки:**
```
404 PRODUCT_NOT_FOUND — товар не найден или не принадлежит пользователю
```

---

### GET /items/summary
Сводка по товарам текущего пользователя: количество по каждому статусу.

**Response `200 OK`:**
```json
{
  "productsByStatus": {
    "IN_CHINA": 5,
    "ON_THE_WAY": 3,
    "IN_KG": 2,
    "DELIVERED": 18
  },
  "activeOrdersCount": 0,
  "deliveredOrdersCount": 0,
  "lastUpdatedAt": "2024-01-16T08:00:00"
}
```

Все статусы всегда присутствуют в `productsByStatus`, даже если count = 0.

`lastUpdatedAt` — время последнего обновления любого товара пользователя (`null` если товаров нет).

> `activeOrdersCount` и `deliveredOrdersCount` в разработке — временно возвращают `0`.

---

## Admin API

### GET /admin/users/{userId}/products
Все товары конкретного пользователя для просмотра в административной панели.

**Auth:** `MANAGER` или `SUPER_ADMIN`

**Query параметры:**

| Параметр | Тип | Описание |
|---|---|---|
| `page` | int | Default `1` |
| `pageSize` | int | Default `50` |

**Response `200 OK`:**
```json
{
  "items": [ ProductAdminResponse ],
  "page": 1,
  "pageSize": 50,
  "total": 63
}
```

Сортировка: `createdAt` DESC.

---

### GET /admin/products/stats
Количество товаров со статусом `ON_THE_WAY` (в пути).

**Auth:** `MANAGER` или `SUPER_ADMIN`

**Query параметры:**

| Параметр | Тип | Описание |
|---|---|---|
| `branchId` | uuid | Опционально. Фильтр по филиалу (только для `SUPER_ADMIN`) |

**Ограничение по роли:** `MANAGER` всегда получает статистику своего филиала.

**Response `200 OK`:**
```json
{
  "count": 127
}
```

---

## Жизненный цикл товара

```
Импорт CN Excel
      ↓
   IN_CHINA  ──── cron 24ч ────→  ON_THE_WAY
                                        ↓
                              Импорт KG Excel (in-kg)
                                        ↓
                                      IN_KG  (создаётся Order)
                                        ↓
                              Импорт KG Excel (delivered)
                                        ↓
                                    DELIVERED
```

Товары создаются и меняют статус исключительно через импорт Excel. Подробности — в `imports.md`.
