---
title: Digital Wallet API
description: What the Digital Wallet API does, how it is built, and where to find each part of its documentation.
---
# Digital Wallet API

A backend for a digital wallet: people register, log in, and then deposit, withdraw and transfer money, with a
full transaction history. Administrators manage users.

## What it is built with

- Spring Boot 4 on Java 25
- PostgreSQL, with plain JDBC (no ORM) and Flyway migrations
- Spring Security with stateless JWT authentication and the roles `USER` and `ADMIN`
- OpenAPI and Swagger UI for the API description
- Docker, Docker Compose and GitHub Actions

## The code is organised by layer

All code lives under `src/main/java/com/app/wallet`:

| Package | Holds |
|---------|-------|
| `controller` | The REST endpoints |
| `service` | The business rules, including the money logic and token handling |
| `repository` | Database access with `JdbcTemplate` |
| `model` and `dto` | The data stored in the database, and the requests and responses of the API |
| `security` and `config` | The JWT filter, security rules and application settings |
| `exception` | Domain exceptions and the single handler that turns them into error responses |

## Where to read next

| Page | Read it for |
|------|-------------|
| [Authentication](authentication.md) | Logging in, tokens, and which endpoints are public |
| [Users and roles](users.md) | Registration, profiles, roles and the admin endpoints |
| [Wallet and transactions](wallet.md) | Balances, deposits, withdrawals, transfers and how money stays consistent |
| [Error responses](errors.md) | The shared JSON error body and every status code |
| [Running and deploying](operations.md) | Settings, Docker, health checks, CI and AWS |

## How these docs are kept current

Each page lists the code it describes in its `sources` header. When a change touches that code without
touching the page, the push is flagged and a coding agent is asked to update the page, so the docs travel in the
same push as the code. This is done by [jarbase](https://github.com/thedistortedwajdan/jarbase).
