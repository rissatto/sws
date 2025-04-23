# 🧠 Architectural Decision Record (ADR)

## Contents

1. [Technology Stack](#1-technology-stack)
2. [Why Flyway?](#2-why-flyway)
3. [Domain-Centric Design & Layering](#3-domain-centric-design--layering)
4. [Idempotency for Operations](#4-idempotency-for-operations)
5. [Event-Sourced Balances](#5-event-sourced-balances)
6. [Concurrency Control](#6-concurrency-control)
7. [Error Handling](#7-error-handling)
8. [Time Handling & Historical Queries](#8-time-handling--historical-queries)
9. [Trade-offs](#9-trade-offs)
10. [Future Improvements](#10-future-improvements)

---

## 1. Technology Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3
- **Persistence**: Spring Data JPA
- **Database**: PostgreSQL (Docker) / H2 for testing
- **Migrations**: Flyway
- **API**: REST + Springdoc OpenAPI (Swagger)
- **Build**: Maven
- **Testing**: JUnit 5, Spring Boot Test

---

## 2. Why Flyway?

We chose **Flyway** as the database migration tool because:

- ✅ Simple, SQL-based migrations
- ✅ Excellent Spring Boot integration
- ✅ Widely adopted
- ❌ Manual rollbacks, acceptable for our scope

---

## 3. Domain-Centric Design & Layering

```
com.rissatto.sws
├── application/     # Services & mappers
├── domain/          # Core business models
├── infrastructure/  # JPA entities, repos, config
└── presentation/    # REST controllers, DTOs, error handlers
```

- Keeps business rules pure
- Separation of responsibilities

---

## 4. Idempotency for Operations

All write endpoints (`create`, `deposit`, `withdraw`, `transfer`) accept an optional `Idempotency-Key` header to:

- Detect duplicate calls
- Return previously created resource or no-op on replay

---

## 5. Event-Sourced Balances

- Every mutation (deposit/withdraw/transfer) writes a `Transaction` record
- **Historical balance** computed by replaying all transactions ≤ requested timestamp
- Benefits:
    - 🔍 Full auditability
    - ⏳ Accurate historical snapshot

---

## 6. Concurrency Control

- Use **pessimistic locking** (`SELECT ... FOR UPDATE`) when loading a wallet for write
- Ensures only one concurrent update per wallet

---

## 7. Error Handling

- Domain exceptions (e.g. `EntityNotFoundException`, insufficient funds) bubble up
- Caught by a `@ControllerAdvice` that maps them to `404`, `400`, etc.

---

## 8. Time Handling & Historical Queries

- Timestamps stored in UTC (`Instant`)
- Endpoint:
  ```
  GET /wallets/{id}/balance?at=2025-04-25T19:00:00Z
  ```  
- Always interpret `at` in UTC—no local‐time conversions.

---

## 9. Trade-offs

- **No auth**: out of scope
- **No balance snapshots**: historical replay on demand (could be slow)
- **No messaging**: direct DB writes, no event bus

---

## 10. Future Improvements

- JWT authentication & authorization
- Metrics/monitoring (Prometheus/Grafana)
- Snapshot background jobs for historical speed
- Migrate to event bus (Kafka) for distributed audit  
