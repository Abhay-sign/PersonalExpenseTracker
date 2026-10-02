# Expense Tracker — Spring Boot + React (learning project)

A deliberately simple app built in stages, to understand how Spring Boot works layer by layer.

## Tech
- Backend: Java 21, Spring Boot 3.5, Spring Data JPA, Flyway, PostgreSQL
- Frontend: React + Vite (stage 4)

## Roadmap
- [x] **Stage 1** — Expense CRUD, validation, global error handling, Flyway, Swagger
- [ ] **Stage 2** — Categories (one-to-many), pagination & sorting, monthly totals query
- [ ] **Stage 3** — Users, Spring Security + JWT, each user sees only their own expenses
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

## API (Stage 1)

| Method | URL                                           | Description            |
|--------|-----------------------------------------------|------------------------|
| GET    | `/api/expenses`                               | List all expenses      |
| GET    | `/api/expenses?from=2026-09-01&to=2026-09-30` | Filter by date range   |
| GET    | `/api/expenses/{id}`                          | Get one                |
| POST   | `/api/expenses`                               | Create                 |
| PUT    | `/api/expenses/{id}`                          | Update                 |
| DELETE | `/api/expenses/{id}`                          | Delete                 |

Example:
```bash
curl -X POST http://localhost:8080/api/expenses \
  -H "Content-Type: application/json" \
  -d '{"title":"Lunch","amount":180.50,"expenseDate":"2026-10-01","note":"Canteen"}'
```

## How a request flows
```
HTTP request
  -> ExpenseController   (URL mapping, @Valid, status codes)
  -> ExpenseService      (business logic, @Transactional)
  -> ExpenseRepository   (Spring Data generates the SQL)
  -> PostgreSQL
Errors anywhere -> GlobalExceptionHandler -> JSON error response
```
