# Digital Wallet API

Spring Boot 4 / Java 17 wallet backend: JWT authentication, role-based authorization, user management,
and a transaction-safe wallet (deposit, withdraw, transfer, history). PostgreSQL + Flyway, plain JDBC.

## Features

| Area | What it does |
|------|--------------|
| Auth | Registration (BCrypt), login, stateless JWT (`sub` = user id), consistent 401/403 JSON errors |
| Roles | `USER` / `ADMIN` (`ROLE_` authorities), `@EnableMethodSecurity`, `@PreAuthorize("hasRole('ADMIN')")` |
| Users | `GET/PUT /api/users/me`, change password; admin list/get/update/role/activate/deactivate/delete |
| Wallet | Balance, deposit, withdraw, transfer by recipient email, paged transaction history |
| Safety | Row locks (`SELECT ... FOR UPDATE`, ordered to avoid deadlocks), single DB transaction per operation, `CHECK (balance >= 0)`, `Idempotency-Key` header |
| Ops | OpenAPI/Swagger UI, `/actuator/health`, Dockerfile + docker-compose, GitHub Actions CI |

## Run locally

Requires JDK 17 and a PostgreSQL database (defaults in `src/main/resources/application.properties`
point at `localhost:5432/walletdb`). Override anything with environment variables:

| Variable | Meaning |
|----------|---------|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | database connection |
| `JWT_SECRET` | base64-encoded HMAC key, at least 256 bits (`openssl rand -base64 48`) |
| `JWT_EXPIRATION` | token lifetime in milliseconds (default `900000`) |
| `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD` | optional: creates the first `ADMIN` at startup if it does not exist |

```bash
./mvnw spring-boot:run
```

Swagger UI: <http://localhost:8080/swagger-ui.html> (spec at `/v3/api-docs`). Use **Authorize** with the
token returned by `POST /api/auth/login`.

## Docker

```bash
export DB_PASSWORD=change-me
export JWT_SECRET=$(openssl rand -base64 48)
docker compose up --build
```

Do not commit secrets; keep them in your shell or an untracked `.env` file (already git-ignored).

## API overview

| Method | Path | Access |
|--------|------|--------|
| POST | `/api/users/register` | public |
| POST | `/api/auth/login` | public |
| GET / PUT | `/api/users/me` | authenticated |
| PUT | `/api/users/me/password` | authenticated |
| GET | `/api/wallet`, `/api/wallet/balance`, `/api/wallet/transactions?page=&size=` | authenticated |
| POST | `/api/wallet/deposit`, `/withdraw`, `/transfer` | authenticated (optional `Idempotency-Key` header) |
| GET | `/api/admin/users`, `/api/admin/users/{id}` | ADMIN |
| PUT | `/api/admin/users/{id}` | ADMIN |
| PATCH | `/api/admin/users/{id}/role`, `/status` | ADMIN |
| DELETE | `/api/admin/users/{id}` | ADMIN (only users with no balance or history) |
| GET | `/actuator/health` | public |

Every error uses the same body:

```json
{
  "timestamp": "2026-01-01T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/users/register",
  "fieldErrors": { "email": "must be a well-formed email address" }
}
```

`401` = missing/invalid/expired token or wrong credentials, `403` = lacks the role or the account is
deactivated, `409` = conflict (duplicate email, idempotency key reuse, admin changing themselves).

### How money stays consistent

* One database transaction per operation; the ledger row(s) and balance update(s) commit or roll back together.
* The wallet row(s) are locked with `SELECT ... FOR UPDATE` before the balance is read, so concurrent
  withdrawals/transfers cannot double-spend. Transfers lock both wallets in ascending id order.
* `wallet_transactions` records `balance_before`, `balance_after`, `status` and a `reference` shared by both sides of a transfer.
* Sending the same `Idempotency-Key` again returns the original result instead of applying the change twice;
  reusing a key for a different request returns `409`.

Notes: tokens are not revoked when a password changes (they expire after `JWT_EXPIRATION`); a deactivated
user is rejected immediately because the account is loaded on every request.

## Tests

```bash
./mvnw test
```

Unit tests use Mockito; integration tests run the full app against in-memory H2 (PostgreSQL mode) through
MockMvc, including concurrency tests for overspending and opposite transfers.

## Deploying to AWS (suggested path)

1. **Image** – build with the `Dockerfile` and push to Amazon ECR.
2. **Database** – Amazon RDS for PostgreSQL; Flyway applies migrations on startup.
3. **Secrets** – store `JWT_SECRET` and the DB password in AWS Secrets Manager / SSM Parameter Store and inject
   them as environment variables into the task (never bake them into the image).
4. **Runtime** – ECS Fargate service behind an Application Load Balancer; target group health check on
   `/actuator/health`.
5. **CI/CD** – `.github/workflows/ci.yml` runs tests and builds the image; extend it with an ECR push and an
   ECS deploy step using GitHub OIDC credentials.
6. **Observability** – ship container logs to CloudWatch and alarm on the ALB 5xx rate and RDS CPU/connections.
