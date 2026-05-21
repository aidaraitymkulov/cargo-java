# Заказы (Orders)

## Обзор

Заказ — агрегирующая сущность, которая объединяет группу товаров одного клиента из одного филиала. Заказы **создаются только через импорт Excel** из КР, вручную не создаются и не редактируются.

В Китае заказов нет — только товары. Заказ появляется когда посылки прибыли в КР и были отсортированы.

---

## Статусы заказа

| Статус | Описание |
|---|---|
| `PENDING_PICKUP` | Заказ создан, ожидает получения клиентом |
| `DELIVERED` | Заказ выдан клиенту |

---

## Модели данных

### OrderResponse (Mobile, список)
```json
{
  "id": "uuid",
  "price": 5000.00,
  "weight": 3.250,
  "itemCount": 3,
  "status": "PENDING_PICKUP",
  "createdAt": "2024-01-20T14:00:00",
  "updatedAt": "2024-01-20T14:00:00"
}
```

### OrderDetailResponse (Mobile, детали)
```json
{
  "id": "uuid",
  "price": 5000.00,
  "weight": 3.250,
  "itemCount": 3,
  "status": "PENDING_PICKUP",
  "products": [
    {
      "id": "uuid",
      "hatch": "YT123456789",
      "status": "IN_KG",
      "orderId": "uuid",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-20T14:00:00"
    }
  ],
  "createdAt": "2024-01-20T14:00:00",
  "updatedAt": "2024-01-20T14:00:00"
}
```

### OrderAdminResponse (Admin)
```json
{
  "id": "uuid",
  "userId": "uuid",
  "branch": {
    "id": "uuid",
    "address": "ул. Киевская 77",
    "personalCodePrefix": "AN",
    "isActive": true
  },
  "price": 5000.00,
  "weight": 3.250,
  "itemCount": 3,
  "status": "DELIVERED",
  "createdAt": "2024-01-20T14:00:00",
  "updatedAt": "2024-01-22T10:00:00"
}
```

---

## Mobile API

Все эндпоинты требуют `Authorization: Bearer <accessToken>`.

### GET /orders
Список заказов текущего пользователя.

**Query параметры:**

| Параметр | Тип | Описание |
|---|---|---|
| `status` | string | Опционально. `PENDING_PICKUP` или `DELIVERED` |
| `page` | int | Default `1` |
| `pageSize` | int | Default `20` |

**Response `200 OK`:**
```json
{
  "items": [ OrderResponse ],
  "page": 1,
  "pageSize": 20,
  "total": 12
}
```

Сортировка: `createdAt` DESC.

**Ошибки:**
```
400 VALIDATION_ERROR — передан неизвестный статус
```

---

### GET /orders/{orderId}
Детали заказа со списком товаров. Возвращает только если заказ принадлежит текущему пользователю.

**Response `200 OK`:** `OrderDetailResponse`

**Ошибки:**
```
404 ORDER_NOT_FOUND — заказ не найден или не принадлежит пользователю
```

---

## Admin API

### GET /admin/users/{userId}/orders
Все заказы конкретного пользователя.

**Auth:** `MANAGER` или `SUPER_ADMIN`

**Query параметры:**

| Параметр | Тип | Описание |
|---|---|---|
| `page` | int | Default `1` |
| `pageSize` | int | Default `20` |

**Response `200 OK`:**
```json
{
  "items": [ OrderAdminResponse ],
  "page": 1,
  "pageSize": 20,
  "total": 7
}
```

Сортировка: `createdAt` DESC.

---

### GET /admin/orders/stats
Количество заказов со статусом `PENDING_PICKUP` (ожидают получения).

**Auth:** `MANAGER` или `SUPER_ADMIN`

**Query параметры:**

| Параметр | Тип | Описание |
|---|---|---|
| `branchId` | uuid | Опционально. Только для `SUPER_ADMIN` |

**Ограничение по роли:** `MANAGER` всегда получает статистику своего филиала.

**Response `200 OK`:**
```json
{
  "count": 43
}
```

---

### GET /admin/orders/revenue
Выручка и количество выданных заказов за указанный месяц.

**Auth:** `MANAGER` или `SUPER_ADMIN`

**Query параметры:**

| Параметр | Тип | Описание |
|---|---|---|
| `branchId` | uuid | Опционально. Только для `SUPER_ADMIN` |
| `year` | int | Год. Default — текущий год |
| `month` | int | Месяц (1–12). Default — текущий месяц |

**Ограничение по роли:** `MANAGER` всегда получает данные своего филиала.

**Response `200 OK`:**
```json
{
  "revenue": 1250000.00,
  "ordersCount": 87
}
```

Учитываются только заказы со статусом `DELIVERED`, созданные в указанном месяце.

---

### GET /admin/orders/delivered-daily
Количество выданных заказов по дням за последние 7 дней (включая сегодня).

**Auth:** `MANAGER` или `SUPER_ADMIN`

**Query параметры:**

| Параметр | Тип | Описание |
|---|---|---|
| `branchId` | uuid | Опционально. Только для `SUPER_ADMIN` |

**Ограничение по роли:** `MANAGER` всегда получает данные своего филиала.

**Response `200 OK`:**
```json
[
  { "date": "2024-01-14", "count": 12 },
  { "date": "2024-01-15", "count": 8 },
  { "date": "2024-01-16", "count": 15 },
  { "date": "2024-01-17", "count": 0 },
  { "date": "2024-01-18", "count": 21 },
  { "date": "2024-01-19", "count": 7 },
  { "date": "2024-01-20", "count": 3 }
]
```

Всегда возвращает ровно 7 элементов в хронологическом порядке. Дни без заказов имеют `count: 0`.

---

## Связь с товарами

- Заказ создаётся при импорте KG Excel (`in-kg`) — см. `imports.md`
- При создании заказа его товары переходят в статус `IN_KG`
- При выдаче заказа (`delivered`) заказ и все его товары переходят в `DELIVERED`
- `products` в `OrderDetailResponse` — полный список товаров заказа
