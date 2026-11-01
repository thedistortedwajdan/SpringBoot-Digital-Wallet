---
title: Authentication
description: How users log in, how JWT access tokens are issued and checked, and which endpoints are public.
sources:
  - src/main/java/com/app/wallet/controller/AuthController.java
  - src/main/java/com/app/wallet/service/AuthService.java
  - src/main/java/com/app/wallet/service/JwtService.java
  - src/main/java/com/app/wallet/service/AuthenticatedUserProvider.java
  - src/main/java/com/app/wallet/security/**
  - src/main/java/com/app/wallet/config/SecurityConfig.java
  - src/main/java/com/app/wallet/config/JwtProperties.java
  - src/main/java/com/app/wallet/dto/LoginUserRequestDto.java
  - src/main/java/com/app/wallet/dto/LoginUserResponseDto.java
---
# Authentication

The API is stateless. A client logs in once, receives a signed JWT access token, and sends it with every
later request in the `Authorization` header:

```
Authorization: Bearer <token>
```

## Logging in

`POST /api/auth/login` with an email and a password.

| Field | Rule |
|-------|------|
| `email` | required, must be a valid email address |
| `password` | required |

A successful login returns the user's `id`, `email`, `firstName`, `lastName` and the `accessToken`.

Two cases fail with an error instead of a token (see [Errors](errors.md)):

- **Wrong email or wrong password** returns `401`. The response is identical for both, so a caller cannot find
  out which emails are registered.
- **A deactivated account** returns `403`, even when the password is correct.

## The token

- It is a JWT signed with an HMAC key. The key comes from the `jwt.secret` setting, which must be a
  base64-encoded value of at least 256 bits.
- The subject (`sub`) is the user's numeric id. The token also carries the issue time and the expiry time.
  It does **not** contain the user's role.
- It is valid for `jwt.expiration` milliseconds after it is issued (see [Operations](operations.md) for the
  setting and its default).
- There is no refresh token. When a token expires the client logs in again.

## How a request is checked

Every request except the public ones goes through `JwtAuthenticationFilter`:

1. Requests to `/api/auth/**` and `/api/users/register` skip the filter completely, so a client that still
   sends an old token can always log in or register.
2. If there is no `Authorization` header, or it uses a scheme other than `Bearer`, the filter does nothing and
   the security rules decide: a protected endpoint then answers `401`.
3. If the header uses `Bearer` but is malformed or empty, the answer is `401` ("Malformed Authorization
   header").
4. The token is verified. An expired token answers `401` ("Token has expired"); any other invalid token
   answers `401` ("Invalid token").
5. The user is loaded from the database **on every request**. If the user no longer exists the answer is `401`;
   if the account is deactivated the answer is `403`.
6. The user's role from the database becomes the request's authority (`ROLE_USER` or `ROLE_ADMIN`).

Because the user and role are read from the database each time, deactivating a user or changing their role takes
effect immediately. The other side of this is that tokens are **not revoked** when a password changes: an old
token stays valid until it expires.

## Which endpoints are public

These need no token:

- `POST /api/auth/login` and anything else under `/api/auth/`
- `POST /api/users/register`
- `POST /api/users/ping`
- The OpenAPI description (`/v3/api-docs/**`) and Swagger UI (`/swagger-ui/**`, `/swagger-ui.html`)
- `GET /actuator/health`

Everything else requires a valid token. The endpoints under `/api/admin` additionally require the `ADMIN`
role, enforced with `@PreAuthorize("hasRole('ADMIN')")` on the controller (method security is enabled).

## Security settings in short

- Sessions are stateless: the server keeps no session between requests.
- CSRF protection, HTTP Basic and form login are all turned off, because the API is token based.
- Passwords are stored as BCrypt hashes.
- Failures raised inside the filter chain (401 and 403) are passed through the same exception handler as
  controller errors, so clients always get the same JSON error body.

## Getting the current user in code

Controllers and services call `AuthenticatedUserProvider` to get the `User` that the filter put into the
security context for the current request.
