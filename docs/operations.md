---
title: Running and deploying
description: Configuration settings, running locally and in Docker, health checks, the OpenAPI docs, CI and the suggested AWS setup.
sources:
  - Dockerfile
  - docker-compose.yml
  - .github/workflows/ci.yml
  - src/main/resources/application.properties
  - src/main/java/com/app/wallet/config/OpenApiConfig.java
  - src/main/java/com/app/wallet/config/JwtProperties.java
  - src/main/java/com/app/wallet/config/AdminBootstrap.java
---
# Running and deploying

The application is a Spring Boot service on Java 25. It stores its data in PostgreSQL and creates its tables
itself with Flyway on startup.

## Settings

Everything can be overridden with environment variables. Defaults live in
`src/main/resources/application.properties`. Never write real secrets into that file or into these docs.

| Variable | Meaning |
|----------|---------|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | The database connection |
| `JWT_SECRET` | The key that signs tokens: base64 encoded, at least 256 bits. A good one comes from `openssl rand -base64 48` |
| `JWT_EXPIRATION` | How long a token lives, in milliseconds (default 600000, which is 10 minutes) |
| `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` | Optional. When both are set, the first administrator is created at startup if it does not exist yet (see [Users](users.md)) |

## Running locally

You need JDK 25 and a PostgreSQL database; the defaults point at `localhost:5432/walletdb`.

```bash
./mvnw spring-boot:run
```

The Swagger UI is at `http://localhost:8080/swagger-ui.html` and the machine-readable description at
`/v3/api-docs`. In the Swagger UI, use **Authorize** with the token from `POST /api/auth/login`. The API
description and the bearer security scheme are defined in `OpenApiConfig`.

## Running with Docker

`docker-compose.yml` starts two services: `db` (PostgreSQL 16, with a health check) and `api` (this application,
built from the `Dockerfile`, which waits until the database is healthy). Set the secrets in your shell first:

```bash
export DB_PASSWORD=change-me
export JWT_SECRET=$(openssl rand -base64 48)
docker compose up --build
```

Keep secrets in your shell or in an untracked `.env` file, which is already git-ignored.

The `Dockerfile` has two stages. The first builds the jar with Maven on a Java 25 JDK image, and the second
runs it on a small Java 25 JRE image as a non-root user, exposing port 8080 with a health check against
`/actuator/health`.

## Health check

`GET /actuator/health` is public and is the only actuator endpoint that is exposed. Liveness and readiness
probes are enabled, and the server shuts down gracefully. Use this path for container and load balancer checks.

## Tests

```bash
./mvnw test
```

Unit tests use Mockito. The integration tests run the whole application against an in-memory H2 database in
PostgreSQL mode, through MockMvc, and include concurrency tests for overspending and for opposite transfers. No
Docker or database is needed to run them.

## Continuous integration

`.github/workflows/ci.yml` runs on every push to `main` and on every pull request. One job builds and tests
the project on Java 25; a second job, which runs only when the first succeeds, builds the Docker image.

## Suggested path to AWS

1. **Image**: build it with the `Dockerfile` and push it to Amazon ECR.
2. **Database**: Amazon RDS for PostgreSQL; Flyway applies the migrations on startup.
3. **Secrets**: keep `JWT_SECRET` and the database password in AWS Secrets Manager or SSM Parameter Store and
   pass them to the task as environment variables. Never bake them into the image.
4. **Runtime**: an ECS Fargate service behind an Application Load Balancer, with the target group health check
   on `/actuator/health`.
5. **CI/CD**: extend the workflow with an ECR push and an ECS deploy step that uses GitHub OIDC credentials.
6. **Observability**: send container logs to CloudWatch and alarm on the load balancer's 5xx rate and on the
   database's CPU and connection count.
