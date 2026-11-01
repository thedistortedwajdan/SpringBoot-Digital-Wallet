---
title: Wallet and transactions
description: Balances, deposits, withdrawals, transfers, the transaction ledger, idempotency keys and how money stays consistent.
sources:
  - src/main/java/com/app/wallet/controller/WalletController.java
  - src/main/java/com/app/wallet/service/WalletService.java
  - src/main/java/com/app/wallet/repository/Wallet*.java
  - src/main/java/com/app/wallet/model/Wallet*.java
  - src/main/java/com/app/wallet/model/TransactionType.java
  - src/main/java/com/app/wallet/dto/MoneyRequestDto.java
  - src/main/java/com/app/wallet/dto/TransferRequestDto.java
  - src/main/java/com/app/wallet/dto/BalanceResponseDto.java
  - src/main/java/com/app/wallet/dto/WalletResponseDto.java
  - src/main/java/com/app/wallet/dto/TransactionResponseDto.java
  - src/main/resources/db/migration/V1__create_tables.sql
  - src/main/resources/db/migration/V2__add_cascade_to_foreign_key_wallet_table.sql
  - src/main/resources/db/migration/V4__create_wallet_transactions.sql
---
# Wallet and transactions

Every user has exactly one wallet, created automatically when they register (see [Users](users.md)). All wallet
endpoints need a token and always act on the wallet of the user the token belongs to.

## Endpoints

| Method and path | What it does |
|-----------------|--------------|
| `GET /api/wallet` | Returns the wallet: `id`, `balance`, `status` and `createdAt` |
| `GET /api/wallet/balance` | Returns just the `balance` |
| `GET /api/wallet/status` | Returns just the wallet `status`, as `{"status": "..."}` |
| `POST /api/wallet/deposit` | Adds money |
| `POST /api/wallet/withdraw` | Takes money out; fails if the balance is too low |
| `POST /api/wallet/transfer` | Sends money to another user, identified by email |
| `GET /api/wallet/transactions` | Lists the wallet's transactions, newest first, paged |

### Amounts

- The `amount` is required and at least `0.01`, with at most 16 digits before the decimal point and 2 after it.
- Balances are stored with exactly two decimal places and can never be negative.
- A `description` is optional, up to 255 characters.
- A deposit or transfer that would make a balance larger than 18 digits in total is rejected with `400`.

### Deposit and withdraw

Both take `amount` and an optional `description`, and return the new transaction. A withdrawal larger than the
balance fails with `400` ("Insufficient balance").

### Transfer

A transfer takes `recipientEmail` (a valid email address), `amount` and an optional `description`. It fails when:

- the recipient email is not registered (`404`)
- the recipient is the sender (`400`)
- the recipient's account is deactivated (`409`)
- the recipient's wallet is not active (`409`)
- the sender's balance is too low (`400`)

On success it returns the sender's side of the transfer.

### Transactions

Each balance change is recorded as a transaction with these fields: `id`, `type`, `amount`, `balanceBefore`,
`balanceAfter`, `status`, `reference`, `counterpartyWalletId`, `description` and `createdAt`.

The `type` is one of `DEPOSIT`, `WITHDRAWAL`, `TRANSFER_OUT` or `TRANSFER_IN`. A transfer writes **two**
transactions, one in each wallet (`TRANSFER_OUT` for the sender and `TRANSFER_IN` for the recipient), and both
share the same `reference`, so they can be matched up. Every transaction is created with the status `COMPLETED`.

The list endpoint takes `page` (starting at 0, default 0) and `size` (default 20, limited to 1 to 100) and
returns `content`, `page`, `size` and `totalElements`.

## Idempotency keys

The three money-moving endpoints accept an optional `Idempotency-Key` header (1 to 64 characters). It makes
retries safe, for example after a network timeout:

- Sending the same key again for the same request returns the **original result** and does not move money a
  second time.
- Reusing a key for a *different* request (another type, amount or recipient) fails with `409`.
- A key is only compared within the same wallet.

Without the header every call is treated as a new request.

## How money stays consistent

- Each operation is one database transaction. The balance update and the ledger rows are saved together or not at
  all.
- Before the balance is read, the wallet row is locked with `SELECT ... FOR UPDATE`, so two requests at the same
  time cannot both spend the same money.
- A transfer locks both wallets, always in ascending order of wallet id, so two opposite transfers cannot
  deadlock each other.
- The database itself enforces the last safety nets: a `CHECK (balance >= 0)` constraint on wallets, a check that
  every transaction amount is positive, a limit on the allowed transaction types, and a unique constraint on
  the wallet and idempotency key together.

## Where the data lives

Two tables hold the wallet data: `wallets` (one row per user, with `balance`, `status` and a link to the user)
and `wallet_transactions` (the ledger described above). The database is changed only through Flyway migrations
in `src/main/resources/db/migration`; see [Operations](operations.md).
