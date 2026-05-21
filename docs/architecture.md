# Архитектура проекта

## Методология

**Модульный монолит** — один деплоируемый артефакт, разбитый на изолированные домены по бизнес-смыслу.

Это не микросервисы и не классический монолит-спагетти. Каждый домен (auth, news, orders…) живёт в своём пакете и не лезет во внутренности другого домена. Взаимодействие — только через публичные сервисы.

**Принципы:**
- **Domain-first packaging** — пакеты по бизнес-домену, не по техническому слою (не `controllers/`, `services/`, а `news/`, `orders/`)
- **Thin controller, fat service** — контроллер только принимает запрос и возвращает ответ; вся логика в сервисе
- **Explicit over implicit** — аннотации безопасности на каждом методе/классе, транзакции явно

---

## Структура пакетов

```
com.cargoapp.backend/
├── common/                  ← переиспользуемые утилиты (нет бизнес-логики)
│   ├── annotation/          ← кастомные аннотации (@CurrentUserId)
│   ├── component/           ← вспомогательные Spring-компоненты
│   ├── dto/                 ← общие DTO (ErrorResponse, PagedResponse)
│   ├── exception/           ← AppException, GlobalExceptionHandler
│   └── resolver/            ← ArgumentResolver-ы
├── config/                  ← глобальная конфигурация (Security, CORS, MVC)
├── auth/
├── users/
├── branches/
├── products/
├── orders/
├── imports/
├── managers/
├── news/
├── notifications/
├── chat/
└── reports/
```

### Структура внутри каждого домена

```
{domain}/
├── controller/
│   ├── mobile/      ← эндпоинты Flutter (URL без префикса)
│   └── admin/       ← эндпоинты React-панели (URL /admin/...)
├── service/         ← бизнес-логика
├── repository/      ← Spring Data JPA
├── entity/          ← JPA-сущности
├── dto/             ← Request и Response классы
├── mapper/          ← маппинг entity ↔ dto
└── config/          ← конфиги, специфичные для домена (если есть)
```

---

## Слой Entity

Entity = таблица в БД. Только хранение данных, никакой логики.

```java
@Entity
@Table(name = "news")
@Getter
@Setter
public class NewsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

**Правила:**
- Lombok `@Getter @Setter` — не `@Data` (генерирует проблемный `equals/hashCode` для JPA)
- `@CreationTimestamp` / `@UpdateTimestamp` — всегда для аудита
- PK — `UUID` с `GenerationType.UUID`
- `nullable = false` — обязательные поля помечать на уровне JPA
- Никаких бизнес-методов в entity
- Enum-поля — `@Enumerated(EnumType.STRING)` (не `ORDINAL`)

---

## Слой Repository

Только интерфейс, наследующий `JpaRepository`. Кастомные запросы — JPQL или нативный SQL.

```java
public interface NewsRepository extends JpaRepository<NewsEntity, UUID> {

    // простой запрос — метод по соглашению Spring Data
    List<NewsEntity> findAllByOrderByCreatedAtDesc();

    // сложный запрос — JPQL
    @Query("SELECT n FROM NewsEntity n WHERE n.title LIKE %:keyword%")
    Page<NewsEntity> searchByTitle(String keyword, Pageable pageable);
}
```

**Правила:**
- Нет реализации — только интерфейс
- Предпочитать методы по соглашению (`findBy...`, `existsBy...`) для простых запросов
- `@Query` — для сложных условий
- Нативный SQL (`nativeQuery = true`) — только если JPQL не справляется

---

## Слой DTO

Отдельные классы для входа (Request) и выхода (Response). Использовать Java records.

```java
// Request — с валидацией
public record CreateNewsRequest(
        @NotBlank String title,
        @NotBlank String content
) {}

// Response — чистые данные, без аннотаций валидации
public record NewsResponse(
        UUID id,
        String image,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
```

**Правила:**
- Entity никогда не возвращать напрямую из контроллера
- Request и Response — всегда разные классы (даже если поля одинаковые)
- Валидация только на Request-классах
- Records для простых DTO; обычный класс — только если нужна мутабельность

---

## Слой Mapper

Ручной маппинг как Spring `@Component`. MapStruct — только для сложных маппингов с повторяющейся логикой.

```java
@Component
public class NewsMapper {

    public NewsResponse toResponse(NewsEntity news) {
        return new NewsResponse(
                news.getId(),
                news.getImage(),
                news.getTitle(),
                news.getContent(),
                news.getCreatedAt(),
                news.getUpdatedAt()
        );
    }
}
```

**Правила:**
- Маппер — `@Component`, инжектится в сервис
- Один маппер на домен
- Метод называется `toResponse`, `toEntity`, `toDto` — по направлению

---

## Слой Service

Весь бизнес-смысл здесь. Сервис работает с repository и mapper, бросает `AppException`.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;

    @Transactional(readOnly = true)
    public NewsResponse getById(UUID id) {
        return newsRepository.findById(id)
                .map(newsMapper::toResponse)
                .orElseThrow(() -> new AppException("NEWS_NOT_FOUND", HttpStatus.NOT_FOUND, "Новость не найдена"));
    }

    @Transactional
    public NewsResponse create(CreateNewsRequest request) {
        var entity = new NewsEntity();
        entity.setTitle(request.title());
        entity.setContent(request.content());
        return newsMapper.toResponse(newsRepository.save(entity));
    }
}
```

**Правила:**
- Все публичные методы — `@Transactional` (мутирующие) или `@Transactional(readOnly = true)` (чтение)
- Ошибки — только через `AppException`, никогда `RuntimeException` напрямую
- `@Slf4j` + `log.warn/error` для нештатных ситуаций (не для ожидаемых ошибок)
- Сервис не знает про HTTP (нет `HttpServletRequest`, нет `ResponseEntity`)
- Не вызывать публичные методы того же сервиса через `this` (транзакции не работают) — выносить в `private`

---

## Слой Controller

Тонкий слой: принять запрос → вызвать сервис → вернуть результат.

```java
@Validated
@RestController
@RequestMapping("/admin/news")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")  // дефолтная роль для всего класса
public class NewsAdminController {

    private final NewsService newsService;

    @GetMapping("/{newsId}")
    public NewsResponse getById(@PathVariable UUID newsId) {
        return newsService.getById(newsId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")             // переопределяем для конкретного метода
    public NewsResponse create(@Valid @RequestBody CreateNewsRequest request) {
        return newsService.create(request);
    }

    @DeleteMapping("/{newsId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void delete(@PathVariable UUID newsId) {
        newsService.delete(newsId);
    }
}
```

**Правила:**
- `@Validated` на классе — чтобы работали `@Min`, `@NotBlank` прямо на `@RequestParam`
- `@Valid` на `@RequestBody` — для валидации вложенных объектов
- `@ResponseStatus` — для 201 Created и 204 No Content; 200 OK по умолчанию
- Никакой бизнес-логики, никаких `if/else` по бизнес-условиям
- Не ловить исключения в контроллере — это задача `GlobalExceptionHandler`
- Mobile-контроллеры: пакет `controller/mobile/`, без `/admin` в URL
- Admin-контроллеры: пакет `controller/admin/`, URL начинается с `/admin/`

### Пагинация в контроллере

```java
@GetMapping
public PagedResponse<NewsResponse> getAll(
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) int pageSize
) {
    return newsService.getAll(page, pageSize);
}
```

В сервисе:

```java
@Transactional(readOnly = true)
public PagedResponse<NewsResponse> getAll(int page, int pageSize) {
    var pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    var result = newsRepository.findAll(pageable);
    return new PagedResponse<>(
            result.getContent().stream().map(newsMapper::toResponse).toList(),
            page,
            pageSize,
            result.getTotalElements()
    );
}
```

---

## Обработка ошибок

### AppException

Единственный способ сигнализировать об ожидаемой ошибке:

```java
throw new AppException("NEWS_NOT_FOUND", HttpStatus.NOT_FOUND, "Новость не найдена");
throw new AppException("FORBIDDEN", HttpStatus.FORBIDDEN, "Нет доступа к этому филиалу");
```

`GlobalExceptionHandler` перехватывает все исключения и оборачивает в единый формат:

```json
{
  "error": "NEWS_NOT_FOUND",
  "message": "Новость не найдена"
}
```

При ошибке валидации `details` заполняется автоматически:

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Ошибка валидации",
  "details": {
    "title": "не должно быть пустым"
  }
}
```

**Правила:**
- Новые error-коды — SCREAMING_SNAKE_CASE, добавить в список в `CLAUDE.md`
- Не бросать `RuntimeException`, `IllegalArgumentException` напрямую — только `AppException`
- Не ловить `AppException` в сервисе, если нечего с ней делать — пусть летит наверх
- `log.error` — только для неожиданных ошибок; ожидаемые (NOT_FOUND, CONFLICT) не логировать

---

## Безопасность

### Аннотирование доступа

```java
// Роль на весь класс
@PreAuthorize("hasRole('MANAGER')")

// Несколько ролей
@PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")

// Переопределение на методе (перекрывает аннотацию класса)
@PreAuthorize("hasRole('SUPER_ADMIN')")
```

### Текущий пользователь

```java
// Получить ID текущего пользователя через кастомный резолвер
public SomeResponse doSomething(@CurrentUserId UUID userId) { ... }

// Или через Spring Security напрямую
public SomeResponse doSomething(@AuthenticationPrincipal UserDetails user) { ... }
```

### Проверка доступа к ресурсу

MANAGER видит только данные своего филиала. Проверку делать в сервисе:

```java
if (!manager.getBranch().getId().equals(targetBranchId)) {
    throw new AppException("FORBIDDEN", HttpStatus.FORBIDDEN, "Нет доступа к этому филиалу");
}
```

---

## Кастомные компоненты в `common/`

### `@CurrentUserId`

Аннотация для параметра метода — извлекает UUID пользователя из JWT без лишнего бойлерплейта.

```java
public OrderResponse getMyOrder(@CurrentUserId UUID userId, @PathVariable UUID orderId) {
    return orderService.getByIdAndUser(orderId, userId);
}
```

### `PagedResponse<T>`

Универсальная обёртка для пагинированных списков. Использовать везде, где нужна пагинация.

### `ErrorResponse`

Возвращается из `GlobalExceptionHandler`. Не создавать вручную в контроллерах.

---

## Миграции БД (Flyway)

Все изменения схемы — только через Flyway. Никаких `ddl-auto: update` в production.

```
src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__add_news_table.sql
└── V3__add_user_chat_ban.sql
```

**Правила:**
- Имя файла: `V{n}__{описание_через_подчёркивание}.sql`
- Версии — строго возрастающие целые числа
- Уже применённый файл **никогда не редактировать** — создать новый `V{n+1}__fix_...sql`
- Новая таблица, новый столбец, новый индекс, переименование — всё через отдельный файл

---

## Антипаттерны — не делать

| Что | Почему нет |
|---|---|
| Бизнес-логика в контроллере | Не тестируется, дублируется |
| Entity из контроллера напрямую | Утечка внутренней схемы, проблемы с lazy loading |
| `@Autowired` на поле | Скрывает зависимости, хуже для тестов — использовать `@RequiredArgsConstructor` |
| Catch `AppException` в сервисе без причины | Скрывает ошибки, ломает единый обработчик |
| `@Transactional` на private методах | Spring не перехватывает — прокси работают только на public |
| Обращение к чужому домену через repository | Только через публичный сервис |
| `log.error` на ожидаемых ошибках (NOT_FOUND) | Засоряет логи, маскирует реальные проблемы |
| `ddl-auto: create/update` | Только `validate` в prod; схему менять через Flyway |
| `@Data` на Entity | Генерирует `equals/hashCode` по всем полям — проблемы с JPA-прокси |
