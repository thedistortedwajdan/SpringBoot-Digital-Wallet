---
title: Error responses
description: The JSON error body every failing request returns, and which situation produces which status code.
sources:
  - src/main/java/com/app/wallet/exception/**
  - src/main/java/com/app/wallet/config/ApiErrorResponses.java
  - src/main/java/com/app/wallet/dto/ApiErrorDto.java
---
# Error responses

Every error, whether it comes from a controller, from validation or from the security filter, is returned as the
same JSON body. One class, `GlobalExceptionHandler`, produces them all, and failures raised inside the security
chain are routed through it too (see [Authentication](authentication.md)).

## The body

| Field | Meaning |
|-------|---------|
| `timestamp` | When the error happened |
| `status` | The HTTP status code |
| `error` | The standard name of the status, such as `Bad Request` |
| `message` | A short explanation for the caller |
| `path` | The request path |
| `fieldErrors` | Only for validation failures: a map from field name to the problem with that field |

Fields that have no value are left out of the response.

## Status codes

| Status | When |
|--------|------|
| `400 Bad Request` | A request body fails validation (`message` is "Validation failed" and `fieldErrors` lists each field), the body is malformed or cannot be read ("Malformed or invalid request"), or a business rule is broken: wrong current password, a new password equal to the old one, transferring to yourself, an amount larger than the balance ("Insufficient balance"), or a balance that would exceed the maximum amount |
| `401 Unauthorized` | Wrong email or password at login ("invalid credentials"); no token ("Authentication is required to access this resource"); or a bad token ("Malformed Authorization header", "Token has expired" or "Invalid token") |
| `403 Forbidden` | The account is deactivated ("Account is deactivated"), or the user's role is not allowed to use the endpoint ("You do not have permission to access this resource") |
| `404 Not Found` | A user, a wallet or a transfer recipient does not exist |
| `409 Conflict` | The request clashes with the current state: an email is already registered, an administrator tries to change their own role or status or delete themselves, a user with wallet activity is deleted, a deactivated recipient, an inactive wallet, or an idempotency key reused for a different request |
| `500 Internal Server Error` | Anything unexpected ("Unexpected error occurred"); details are written to the server log only, never to the response |

Errors raised by Spring itself that carry their own status, for example a request to an unknown path or with an
unsupported method, keep that status. For client errors the `message` is the standard name of the status.

## Adding a new error

Throw one of the existing exceptions from `src/main/java/com/app/wallet/exception`. Pick by the status you want:
`BadRequestException` (400), `ResourceNotFoundException` or `UserDoesNotExistException` (404),
`ConflictException` or `EmailAlreadyExistsException` (409), `InvalidCredentialsException` (401) and
`AccountDisabledException` (403). Only add a new handler to `GlobalExceptionHandler` when none of them fits.

Controllers show these responses in the Swagger UI automatically, through the `@ApiErrorResponses` annotation.
