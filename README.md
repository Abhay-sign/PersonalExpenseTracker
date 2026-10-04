# Expense Tracker — Spring Boot + React (learning project)

A deliberately simple app built in stages, to understand how Spring Boot works layer by layer.

## Tech
- Backend: Java 21, Spring Boot 3.5, Spring Data JPA, Flyway, PostgreSQL
- Frontend: React + Vite (stage 4)

## Roadmap
- [x] **Stage 1** — Expense CRUD, validation, global error handling, Flyway, Swagger
- [x] **Stage 2** — Categories (one-to-many), pagination & sorting, monthly totals query
- [x] **Stage 3** — Users, Spring Security + JWT, each user sees only their own expenses
- [ ] **Stage 4** — React frontend (login, dashboard, add/edit expense), CORS
- [ ] **Stage 5** — Tests (JUnit, Mockito, integration test), profiles (dev/prod)

## Run locally

1. Start PostgreSQL (needs Docker):
   ```bash
   docker compose up -d
   ```
2. Run the backend:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   (Or open `backend/` in IntelliJ and run `ExpenseTrackerApplication`.)
3. Open Swagger UI: http://localhost:8080/swagger-ui.html

## API

**Auth (Stage 3):** every `/api/expenses` and `/api/categories` call needs `Authorization: Bearer <token>`.
Get a token from the public endpoints below. Each user only sees their own expenses (categories are shared).
In Swagger UI use the **Authorize** button and paste the token.

| Method | URL                      | Description                              |
|--------|--------------------------|------------------------------------------|
| POST   | `/api/auth/register`     | `{email, password}` -> 201 + token (409 if email taken) |
| POST   | `/api/auth/login`        | `{email, password}` -> token (401 if wrong) |


| Method | URL                                  | Description                                   |
|--------|--------------------------------------|-----------------------------------------------|
| GET    | `/api/expenses`                      | Paged list (see query params below)           |
| GET    | `/api/expenses/summary/monthly`      | Monthly totals, `?year=2026` (default: current) |
| GET    | `/api/expenses/{id}`                 | Get one                                       |
| POST   | `/api/expenses`                      | Create (optional `categoryId`)                |
| PUT    | `/api/expenses/{id}`                 | Update                                        |
| DELETE | `/api/expenses/{id}`                 | Delete                                        |
| GET    | `/api/categories`                    | List categories                               |
| GET    | `/api/categories/{id}`               | Get one                                       |
| POST   | `/api/categories`                    | Create (409 if name exists)                   |
| PUT    | `/api/categories/{id}`               | Rename                                        |
| DELETE | `/api/categories/{id}`               | Delete (409 if it still has expenses)         |

`GET /api/expenses` query params (all optional): `page` (0-based), `size` (default 20, max 100),
`sort` (e.g. `amount,desc`; default `expenseDate,desc`), `from`, `to` (dates), `categoryId`.
The response is `{ content, page, size, totalElements, totalPages }`.

Examples:
```bash
curl -X POST http://localhost:8080/api/expenses \
  -H "Content-Type: application/json" \
  -d '{"title":"Lunch","amount":180.50,"expenseDate":"2026-10-01","note":"Canteen","categoryId":1}'

curl "http://localhost:8080/api/expenses?categoryId=1&sort=amount,desc&page=0&size=10"
curl "http://localhost:8080/api/expenses/summary/monthly?year=2026"
```

Set `JWT_SECRET` (32+ chars) in any real deployment; a dev default is used otherwise.
Migration `V3` adds users; expenses from Stages 1-2 have no owner and are not visible to any user.

Migration `V2` seeds five categories (Food, Transport, Rent, Entertainment, Other).

## How a request flows
```
HTTP request
  -> ExpenseController   (URL mapping, @Valid, status codes)
  -> ExpenseService      (business logic, @Transactional)
  -> ExpenseRepository   (Spring Data generates the SQL)
  -> PostgreSQL
Errors anywhere -> GlobalExceptionHandler -> JSON error response
```
