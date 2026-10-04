# Expense Tracker — Study Notes (Stages 1–2)

## 1. Big picture
A REST API for tracking expenses, built in stages. Backend only so far.

- **Stack:** Java 21, Spring Boot 3.5.6, Spring Web, Spring Data JPA (Hibernate), Bean Validation, Flyway, PostgreSQL 16, springdoc (Swagger UI), Maven.
- **Package root:** `com.abhay.expensetracker`
- **Layout:** feature packages (`expense`, `category`) plus `common` for shared code.

```
backend/src/main/java/com/abhay/expensetracker/
  ExpenseTrackerApplication.java
  common/    GlobalExceptionHandler, ResourceNotFoundException, ConflictException, PageResponse
  expense/   Expense, ExpenseController/Service/Repository, ExpenseSpecifications, dto/
  category/  Category, CategoryController/Service/Repository, dto/
backend/src/main/resources/
  application.yml
  db/migration/  V1__create_expenses.sql, V2__create_categories.sql
docker-compose.yml   (Postgres)
```

## 2. Request flow (layered architecture)
```
HTTP -> Controller -> Service -> Repository -> PostgreSQL
errors anywhere -> GlobalExceptionHandler -> JSON (ProblemDetail)
```
| Layer | Responsibility | Must NOT do |
|---|---|---|
| Controller | URLs, `@Valid`, status codes, JSON | business logic |
| Service | business rules, `@Transactional`, entity <-> DTO | HTTP concerns |
| Repository | DB access (interface only; Spring generates it) | business logic |
| Entity | maps to a table | be sent/received as JSON directly |
| DTO (records) | shape of request/response JSON | hold DB state |

## 3. Setup and running
- `docker-compose.yml`: Postgres 16, db `expense_tracker`, user `expense_user`, password `expense_pass`, port 5432, named volume `pgdata` (data survives restarts).
- `docker compose up -d`, then `cd backend && mvn spring-boot:run`.
- Swagger UI: http://localhost:8080/swagger-ui.html
- `application.yml` uses `${ENV_VAR:default}` so DB settings can be overridden by env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`); defaults match docker-compose.
- Key config:
  - `ddl-auto: validate` — Hibernate only *checks* entities against the schema; Flyway owns the schema.
  - `open-in-view: false` — no lazy loading outside the service layer (avoids hidden queries in controllers).
  - `show-sql` + `format_sql` — see the generated SQL in the console (great for learning).
  - `hibernate.jdbc.time_zone: UTC`.
  - `spring.data.web.pageable`: default page size 20, max 100.
- `ExpenseTrackerApplication` sets the JVM default timezone to UTC in `main`. Reason: Windows reports IST as legacy `Asia/Calcutta`, which Postgres rejects.

## 4. Database and Flyway
- Flyway runs `V*__*.sql` files in order on startup and records them in `flyway_schema_history`.
- **Never edit a migration that has already run** — add a new `V3__...` instead (Flyway checks checksums and will fail).
- **V1** `expenses`: `id BIGSERIAL PK`, `title VARCHAR(100)`, `amount NUMERIC(12,2) CHECK (amount > 0)`, `expense_date DATE`, `note VARCHAR(500)`, `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`; index on `expense_date`.
- **V2**: `categories(id, name UNIQUE VARCHAR(50))`; `expenses.category_id BIGINT` FK (nullable) + index; seeds Food, Transport, Rent, Entertainment, Other.
- Naming: Java `expenseDate` -> column `expense_date` automatically (Spring's snake_case naming strategy).
- Money is `BigDecimal` / `NUMERIC`, never `double`.

## 5. Entities (JPA)
**Expense** (`expenses`)
- `@Id @GeneratedValue(IDENTITY)` — DB generates the id.
- `@PrePersist` sets `createdAt`/`updatedAt`; `@PreUpdate` refreshes `updatedAt`.
- `protected` no-arg constructor for JPA; public constructor for our code; getters, and setters only for mutable fields (no `setId`, no `setCreatedAt`).
- `@ManyToOne(fetch = LAZY) @JoinColumn(name = "category_id") Category category` — optional.

**Category** (`categories`): `id`, `name` (unique).

**One-to-many, as implemented:** one category has many expenses, but the relationship is mapped only from the "many" side (`Expense.category`). The foreign key lives in `expenses`, so `Category` needs no collection. This is simpler and avoids loading huge lists by accident. `LAZY` means the category row loads only when accessed.

## 6. DTOs and validation
- **ExpenseRequest**: `title` (`@NotBlank`, max 100), `amount` (`@NotNull`, `@DecimalMin 0.01`, `@Digits(10,2)`), `expenseDate` (`@NotNull`, `@PastOrPresent`), `note` (max 500), `categoryId` (optional).
- **ExpenseResponse**: id, title, amount, expenseDate, note, `category {id, name}` or null, createdAt, updatedAt. Built by `ExpenseResponse.from(entity)`.
- **CategoryRequest**: `name` (`@NotBlank`, max 50). **CategoryResponse**: `id`, `name`.
- **MonthlyTotal**: `year, month, total, count`.
- **PageResponse<T>**: `content, page, size, totalElements, totalPages`.
- Why DTOs: clients can't set `id`/`createdAt`; the API shape is independent of the DB; no lazy-loading or serialization surprises.
- Java **records** are used for DTOs: immutable, concise.

## 7. API reference
**Expenses** — `/api/expenses`
| Method | URL | Notes |
|---|---|---|
| GET | `/api/expenses` | paged + filtered (below) |
| GET | `/api/expenses/summary/monthly?year=` | monthly totals; defaults to current year |
| GET | `/api/expenses/{id}` | 404 if missing |
| POST | `/api/expenses` | 201 + `Location` header |
| PUT | `/api/expenses/{id}` | full replace; 404 if missing |
| DELETE | `/api/expenses/{id}` | 204 |

List query params (all optional): `page` (0-based), `size` (default 20, max 100), `sort` (`field,asc|desc`, repeatable; default `expenseDate,desc`), `from`, `to` (ISO dates, inclusive), `categoryId`.

**Categories** — `/api/categories`: GET list (sorted by name), GET `{id}`, POST (201), PUT `{id}` (rename), DELETE `{id}` (204).

## 8. Error handling (`GlobalExceptionHandler`)
`@RestControllerAdvice` converts exceptions to RFC 9457 `ProblemDetail` JSON (`title`, `status`, `detail`).
| Exception | Status | When |
|---|---|---|
| `ResourceNotFoundException` | 404 | unknown expense/category id (including an unknown `categoryId` in a request) |
| `MethodArgumentNotValidException` | 400 | validation failed; adds `errors: {field: message}` |
| `ConflictException` | 409 | duplicate category name; deleting a category that still has expenses |
| `PropertyReferenceException` | 400 | `?sort=` on a field that doesn't exist |

## 9. Spring concepts used (and where)
- **`@SpringBootApplication`**: component scanning + auto-configuration (sees Postgres/JPA on the classpath and wires DataSource, EntityManager, etc.).
- **Dependency injection**: constructor injection everywhere, no `@Autowired` needed with a single constructor.
- **`@Transactional`** on service classes; `readOnly = true` on reads (lets Hibernate skip dirty checking).
- **Dirty checking**: `ExpenseService.update` never calls `save()`; the entity is managed in the transaction, so Hibernate issues the UPDATE on commit.
- **Derived queries** (Stage 1): `findByExpenseDateBetweenOrderByExpenseDateDesc` — Spring builds SQL from the method name. Replaced in Stage 2 by Specifications, since filters became optional and combinable. `existsByCategoryId`, `existsByNameIgnoreCase`, `findAllByOrderByNameAsc` are still derived queries.
- **Specifications** (`JpaSpecificationExecutor`, `ExpenseSpecifications`): each filter is a small WHERE fragment; a `null` spec is ignored. Combined with `Specification.where(...).and(...)`. Works together with `Pageable`.
- **Pagination/sorting**: Spring injects a `Pageable` from `page/size/sort`; `@PageableDefault` sets defaults; `findAll(spec, pageable)` returns a `Page` (runs a data query + a count query).
- **JPQL with `@Query`** for the monthly report: `select new ...MonthlyTotal(year(..), month(..), sum(..), count(..)) ... group by ... order by ...`. "Constructor expression" builds the DTO directly from result rows.
- **`ResponseEntity.created(location)`** for 201 + `Location` header; `@ResponseStatus(NO_CONTENT)` for 204.
- **springdoc** generates OpenAPI docs and Swagger UI automatically from the controllers.

## 9b. Design decisions worth remembering
- Category with expenses can't be deleted (409) rather than cascading — protects data. The DB FK would also reject it; the service check gives a friendly message.
- Category names are unique case-insensitively (checked in the service) and also by DB constraint (exact match).
- Updates are full replace (PUT); sending no `categoryId` clears the category.
- The list endpoint returns our own `PageResponse` instead of Spring's `Page` to keep the JSON stable.
- Monthly totals only include months that have expenses; the date range for a year is Jan 1 – Dec 31.
- `Sort` on `category.name` etc. is not special-cased; sorting uses entity property names (`title`, `amount`, `expenseDate`, `createdAt`, ...).

## 10. Stage 1 -> Stage 2 breaking changes
- `GET /api/expenses` returns a page object, not a plain array.
- `ExpenseRequest`/`ExpenseResponse` gained the category field (backward compatible for clients that ignore it).
- `Expense` constructor takes a `Category` (can be `null`).

## 11. Try it
```bash
curl -X POST localhost:8080/api/categories -H "Content-Type: application/json" -d '{"name":"Health"}'
curl -X POST localhost:8080/api/expenses -H "Content-Type: application/json" \
  -d '{"title":"Lunch","amount":180.50,"expenseDate":"2026-10-01","note":"Canteen","categoryId":1}'
curl "localhost:8080/api/expenses?categoryId=1&sort=amount,desc&page=0&size=10"
curl "localhost:8080/api/expenses/summary/monthly?year=2026"
curl "localhost:8080/api/expenses?sort=bogus"          # 400
curl -X DELETE localhost:8080/api/categories/1          # 409 if it has expenses
```

## 12. Status and what's next
- Done: Stage 1 (CRUD, validation, error handling, Flyway, Swagger), Stage 2 (categories, pagination/sorting/filtering, monthly totals).
- Stage 2 code compiles; it has **not been run against a live database yet** — verify the V2 migration and the monthly query on first startup.
- No automated tests yet (planned for Stage 5; `spring-boot-starter-test` is already a dependency).
- Next: **Stage 3** — users, Spring Security + JWT, per-user expenses (will need a `V3` migration adding `user_id`). **Stage 4** — React + Vite frontend, CORS. **Stage 5** — JUnit/Mockito/integration tests, dev/prod profiles.

---

# Stage 3 — Users, Spring Security + JWT

## Flow
```
POST /api/auth/register|login -> AuthService -> JwtService.generateToken -> { token }
any other request -> JwtAuthenticationFilter (reads "Authorization: Bearer ...")
   valid token  -> SecurityContext holds AuthenticatedUser(id, email)
   no/bad token -> 401
Controller gets @AuthenticationPrincipal AuthenticatedUser -> service filters every query by user id
```

## New pieces
- `user/User`, `UserRepository`; migration `V3` (`users` table, `expenses.user_id` FK).
- `security/SecurityConfig`: stateless sessions, CSRF off (no cookies), `/api/auth/**` and Swagger public, everything else authenticated, 401 entry point, BCrypt `PasswordEncoder` bean.
- `security/JwtService` (jjwt 0.12): HS256 token with `sub`=user id, `email`, `iat`, `exp`. Secret from `app.jwt.secret` (env `JWT_SECRET`, must be 32+ chars; app refuses to start otherwise). Expiry `app.jwt.expiration-minutes` (default 60).
- `security/JwtAuthenticationFilter` (`OncePerRequestFilter`) runs before `UsernamePasswordAuthenticationFilter`.
- `security/OpenApiConfig`: Authorize button in Swagger.
- `auth/` : `AuthController`, `AuthService`, DTOs (`RegisterRequest` email + password 8-72 chars, `LoginRequest`, `AuthResponse`).

## Concepts / decisions
- **Hashing vs encryption:** passwords are stored as BCrypt hashes (salted, slow, one-way). Login = `matches(raw, hash)`.
- **JWT** = header.payload.signature. Signed, not encrypted: anyone can read the payload, nobody can forge it. Stateless: the server stores no sessions, so logout = client discards the token (no revocation until expiry).
- **Authorization at the data layer:** `ownedBy(userId)` specification, `findByIdAndUserId`, and `where e.user.id = :userId` in the monthly query. Someone else's expense returns **404**, not 403, so ids can't be probed.
- Same error for unknown email and wrong password (no user enumeration). Emails are trimmed + lowercased.
- Categories stay **global/shared** (seeded in V2); any logged-in user can manage them. Making them per-user would be a later change.
- `userRepository.getReferenceById(userId)` gives an id-only proxy for the FK without an extra SELECT.
- `Expense.user` is `updatable = false`: ownership can't change.
- Old Stage 1/2 expenses have `user_id = NULL` (column nullable for that reason) and are invisible to everyone.
- New error: `BadCredentialsException` -> 401 via `GlobalExceptionHandler`. Missing/invalid token -> bare 401 from the security entry point.

## Try it
```bash
curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"email":"a@x.com","password":"password123"}'
TOKEN=<token from response>
curl localhost:8080/api/expenses -H "Authorization: Bearer $TOKEN"
curl localhost:8080/api/expenses            # 401
```
