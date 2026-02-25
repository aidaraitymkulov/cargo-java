# База данных — AdesExpress

СУБД: **PostgreSQL**
Миграции: **Flyway** (`src/main/resources/db/migration/`)

## Схема

### users
| Поле | Тип | Описание |
|---|---|---|
| id | uuid PK | |
| email | varchar UNIQUE | |
| password_hash | varchar | |
| first_name | varchar | |
| last_name | varchar | |
| phone | varchar | |
| date_of_birth | date | |
| personal_code | varchar UNIQUE | Текущий активный (AN0001) |
| branch_id | uuid FK→branches | |
| role_id | uuid FK→user_roles | |
| chat_banned | boolean default false | |
| status | int | 0=ACTIVE, 1=INACTIVE, 2=DELETED, 3=PENDING_DELETION |
| created_at | timestamp | |
| updated_at | timestamp | |

### user_roles
| Поле | Тип |
|---|---|
| id | uuid PK |
| role_name | varchar UNIQUE | `USER`, `MANAGER`, `SUPER_ADMIN` |

### user_personal_codes
История всех personalCode пользователя (при смене филиала).
| Поле | Тип |
|---|---|
| id | uuid PK |
| user_id | uuid FK→users |
| personal_code | varchar |
| branch_id | uuid FK→branches |
| is_active | boolean default false |
| created_at | timestamp |

---

### branches
| Поле | Тип | Описание |
|---|---|---|
| id | uuid PK | |
| address | varchar | |
| personal_code_prefix | varchar | `AN`, `D`, `OW` и т.п. |
| is_active | boolean default true | |
| next_sequence | int default 1 | Счётчик для генерации кодов |
| created_at | timestamp | |
| updated_at | timestamp | |

**Генерация personalCode:** `prefix + next_sequence` (нужна блокировка строки при инкременте)

---

### products
| Поле | Тип | Описание |
|---|---|---|
| id | uuid PK | |
| hatch | varchar | Трекинг-номер (YT..., JT...) |
| user_id | uuid FK→users | |
| order_id | uuid FK→orders NULLABLE | null пока нет заказа |
| status | varchar | IN_CHINA, ON_THE_WAY, IN_KG, DELIVERED |
| created_at | timestamp | |
| updated_at | timestamp | |

### orders
| Поле | Тип | Описание |
|---|---|---|
| id | uuid PK | |
| user_id | uuid FK→users | |
| branch_id | uuid FK→branches | |
| price | numeric(12,2) | |
| weight | numeric(12,3) | |
| item_count | int | |
| status | varchar | PENDING_PICKUP, DELIVERED |
| created_at | timestamp | |
| updated_at | timestamp | |

### product_histories
Таймлайн изменений статуса товара.
| Поле | Тип |
|---|---|
| id | uuid PK |
| product_id | uuid FK→products |
| status | varchar |
| created_at | timestamp |

---

### refresh_sessions
| Поле | Тип | Описание |
|---|---|---|
| id | uuid PK | |
| user_id | uuid FK→users | |
| jti | varchar UNIQUE | JWT ID |
| fingerprint | varchar | |
| ip | varchar | |
| user_agent | text | |
| created_at | timestamp | |
| expires_at | timestamp | |
| revoked_at | timestamp NULLABLE | null = активная |

### confirmations
| Поле | Тип | Описание |
|---|---|---|
| id | uuid PK | |
| code | varchar(12) | 6-значный код |
| confirmation_status | varchar | |
| attempts | int default 3 | |
| expires_at | timestamp | |
| last_sent_at | timestamp | |
| user_id | uuid FK→users | |

---

### news
| Поле | Тип |
|---|---|
| id | uuid PK |
| cover | varchar NULLABLE | URL картинки |
| title | varchar |
| content | text |
| created_at | timestamp |
| updated_at | timestamp |

### push_tokens
| Поле | Тип | Описание |
|---|---|---|
| id | serial PK | |
| user_id | uuid FK→users | |
| token | text | FCM/APNS токен |
| created_at | timestamp | |
| updated_at | timestamp | |

Невалидные токены (InvalidToken, NotRegistered от FCM/APNS) **автоматически удаляются** бэкендом.

---

### chat_messages
| Поле | Тип | Описание |
|---|---|---|
| id | uuid PK | |
| user_id | uuid FK→users | Всегда клиент |
| manager_id | uuid FK→users NULLABLE | null если отправитель — клиент |
| sender_type | varchar | `USER` или `MANAGER` |
| message | text | |
| is_read | boolean default false | |
| created_at | timestamp | |

### notification_logs
Внутреннее логирование отправленных уведомлений. API-эндпоинта нет.
| Поле | Тип | Описание |
|---|---|---|
| id | uuid PK | |
| type | varchar | TEST, BROADCAST, BY_PERSONAL_CODE |
| title | varchar | |
| body | text | |
| data_json | json | |
| code_prefix | varchar NULLABLE | для BY_PERSONAL_CODE |
| target_user_id | uuid NULLABLE | для TEST |
| created_by_id | uuid FK→users | |
| created_at | timestamp | |

---

## Связи
```
users → branches          (many-to-one)
users → user_roles        (many-to-one)
user_personal_codes → users, branches
products → users          (many-to-one)
products → orders         (many-to-one, nullable)
orders → users, branches  (many-to-one)
product_histories → products
refresh_sessions → users
confirmations → users
push_tokens → users
chat_messages → users (×2: user_id, manager_id)
notification_logs → users (×2: target_user_id, created_by_id)
```

## Миграции
```
src/main/resources/db/migration/
├── V1__init_schema.sql       — базовая схема
├── V2__...sql                — следующие изменения
```

Правило именования: `V{номер}__{описание}.sql`
