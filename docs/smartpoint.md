# SmartPoint Integration

SmartPoint — логистический партнёр, который ведёт посылки от склада в Китае до выдачи в КГ.

## Переменные окружения

```
SMARTPOINT_API_TOKEN=<токен для исходящих запросов>
SMARTPOINT_WEBHOOK_SECRET=<секрет для валидации входящих вебхуков>
```

---

## 1. Синхронизация клиента

### Когда происходит
После подтверждения email пользователем (`POST /auth/confirm`).

### Что отправляем
```
POST https://smartpoint.kg/api/v1/ades/clients/upsert
Authorization: Bearer <SMARTPOINT_API_TOKEN>
Content-Type: application/json

{
  "client_code": "BSK0001",
  "phone":       "+996700123456",
  "first_name":  "Иван",
  "last_name":   "Иванов"
}
```

`client_code` = `personalCode` пользователя (генерируется при регистрации по префиксу филиала).

### Retry логика
- Одна попытка сразу при подтверждении (timeout 5 сек)
- Если не удалось — `smartpoint_synced_at` остаётся NULL
- Cron каждые 30 минут повторяет попытку для всех подтверждённых пользователей у которых `smartpoint_synced_at IS NULL`

---

## 2. Webhook — обновление статусов посылок

SmartPoint присылает уведомление при каждом изменении статуса посылки.

### Эндпоинт
```
POST /webhook/smartpoint/status/{trackNumber}
Header: x-ades-token: <SMARTPOINT_WEBHOOK_SECRET>
```

### Тело запроса
```json
{
  "status":      "ON_WAY",
  "client_code": "BSK0001",
  "weight":      1.25,
  "price":       850.00,
  "comment":     "optional"
}
```

### Маппинг статусов

| SmartPoint статус | Наш статус   | Описание                    |
|-------------------|--------------|-----------------------------|
| `AWAIT_POSTING`   | `IN_CHINA`   | Посылка принята на складе в Китае |
| `ON_WAY`          | `ON_THE_WAY` | Едет из Китая в КГ          |
| `ON_PVZ`          | `IN_KG`      | Прибыла в Кыргызстан        |
| `ISSUED`          | `DELIVERED`  | Выдана клиенту              |

### Логика обработки
1. Валидация `x-ades-token`
2. Маппинг статуса — если неизвестный, пропускаем
3. Ищем товар по `trackNumber` + `client_code` (personalCode) со статусом не DELIVERED
4. **Товар найден** → обновляем статус, weight, price
5. **Товар не найден** → создаём новый товар с пришедшим статусом
6. Записываем в историю (`product_history`)

---

## 3. Cron — автоперевод IN_CHINA → ON_THE_WAY

Каждые 12 часов все товары со статусом `IN_CHINA` старше 12 часов автоматически переводятся в `ON_THE_WAY`.

Это страховка на случай если SmartPoint не прислал `ON_WAY` webhook.

---

## Полный флоу посылки

```
[Регистрация]
Клиент регистрируется → personalCode = BSK0001
Подтверждает email → BSK0001 синкается в SmartPoint

[Китай]
Клиент заказывает товар, указывает адрес склада SmartPoint
Пишет BSK0001 на посылке
Посылка приходит на склад → SmartPoint шлёт AWAIT_POSTING
  → создаётся товар IN_CHINA

[В пути]
SmartPoint грузит в фуру → шлёт ON_WAY
  → статус меняется на ON_THE_WAY
  (или cron через 12ч если webhook не пришёл)

[Кыргызстан]
Фура прибыла → SmartPoint шлёт ON_PVZ
  → статус меняется на IN_KG

[Выдача]
Клиент забирает → SmartPoint шлёт ISSUED
  → статус меняется на DELIVERED
```