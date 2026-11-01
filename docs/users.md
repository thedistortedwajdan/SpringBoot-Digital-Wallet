---
title: Users and roles
description: Registration, the profile endpoints, the USER and ADMIN roles, and what administrators can do.
sources:
  - src/main/java/com/app/wallet/controller/UserController.java
  - src/main/java/com/app/wallet/controller/AdminUserController.java
  - src/main/java/com/app/wallet/service/UserService.java
  - src/main/java/com/app/wallet/repository/UserRepository.java
  - src/main/java/com/app/wallet/mapper/UserRowMapper.java
  - src/main/java/com/app/wallet/model/User.java
  - src/main/java/com/app/wallet/model/Role.java
  - src/main/java/com/app/wallet/config/AdminBootstrap.java
  - src/main/java/com/app/wallet/dto/RegisterUserRequestDto.java
  - src/main/java/com/app/wallet/dto/UpdateUserRequestDto.java
  - src/main/java/com/app/wallet/dto/ChangePasswordRequestDto.java
  - src/main/java/com/app/wallet/dto/ChangeRoleRequestDto.java
  - src/main/java/com/app/wallet/dto/ChangeStatusRequestDto.java
  - src/main/java/com/app/wallet/dto/UserResponseDto.java
  - src/main/java/com/app/wallet/dto/PageResponseDto.java
  - src/main/resources/db/migration/V3__add_user_status_and_role_constraint.sql
---
# Users and roles

## Roles

There are two roles, stored on the user as text and checked by a database constraint:

| Role | What it can do |
|------|----------------|
| `USER` | Manage their own profile and password, and use their own wallet |
| `ADMIN` | Everything a user can do, plus the endpoints under `/api/admin/users` |

A user also has an `active` flag. A deactivated user cannot log in and is rejected on every request, even with a
token that has not expired yet (see [Authentication](authentication.md)).

## Registering

`POST /api/users/register` is public and answers `201 Created` with no body.

| Field | Rule |
|-------|------|
| `firstName` | required, not blank |
| `lastName` | required, not blank |
| `email` | required, a valid email address; must not be registered already |
| `password` | required, between 8 and 100 characters |

Registration does three things in a single database transaction: it stores the user with the `USER` role and a
BCrypt password hash, and it creates the user's wallet (see [Wallet](wallet.md)). If the email is taken the
answer is `409`, including when two requests race for the same email.

## Your own profile

These need a valid token and always act on the user the token belongs to:

| Method and path | What it does |
|-----------------|--------------|
| `GET /api/users/me` | Returns the current user |
| `PUT /api/users/me` | Updates `firstName`, `lastName` and `email` (each required, names up to 100 characters, email up to 255). Changing to an email that someone else uses returns `409`. |
| `PUT /api/users/me/password` | Changes the password; answers `204` |

Changing the password needs `currentPassword` and a `newPassword` of 8 to 100 characters. The request fails with
`400` if the current password is wrong, or if the new password is the same as the current one.

## Administration

All endpoints below require the `ADMIN` role; a normal user gets `403`.

| Method and path | What it does |
|-----------------|--------------|
| `GET /api/admin/users` | Lists users, paged |
| `GET /api/admin/users/{id}` | Returns one user (`404` if it does not exist) |
| `PUT /api/admin/users/{id}` | Updates a user's name and email, with the same rules as the profile update |
| `PATCH /api/admin/users/{id}/role` | Sets the role to `USER` or `ADMIN` |
| `PATCH /api/admin/users/{id}/status` | Activates or deactivates the account (`active`: true or false) |
| `DELETE /api/admin/users/{id}` | Deletes a user; answers `204` |

Rules that protect against accidents:

- An administrator **cannot change their own role or status, or delete their own account**. These return `409`.
- A user can only be **deleted when they have no wallet activity**, meaning no balance and no transaction history.
  Otherwise the answer is `409` and the message suggests deactivating the account instead.

### Paging

The list endpoint takes `page` (starting at 0, default 0) and `size` (default 20). Sizes are limited to the range
1 to 100, and values outside the range are corrected silently. The response has `content` (the users on this
page), `page`, `size` and `totalElements`.

A user is returned as `id`, `email`, `firstName`, `lastName`, `role`, `active` and `createdAt`. The password hash
is never part of a response.

## The first administrator

No user can make themselves an administrator through the API. To get the first one, set `app.admin.email` and
`app.admin.password` (the environment variables `APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD`). When both are
set, the application creates that administrator and a wallet for them at startup, unless a user with that email
already exists. When either is empty nothing happens. See [Operations](operations.md).
