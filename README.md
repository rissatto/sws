# 💰 Simple Wallet Service

A Java-based microservice for managing user wallets with support for deposits, withdrawals, p2p transfers, and historical balance queries.  
See the [Architectural Decision Record (ADR.md)](ADR.md) for all design and trade-off decisions.

---

## 🚀 Features

- Create users and wallets
- Deposit and withdraw funds
- Transfer funds between wallets
- Retrieve current and historical balances (`at` query param)
- Full audit log of all operations
- API documentation via Swagger/OpenAPI

---

## 🛠️ Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3
- **Persistence**: Spring Data JPA
- **Database**: PostgreSQL (Docker) / H2 (tests)
- **Migrations**: Flyway
- **API Docs**: Springdoc OpenAPI (Swagger UI)
- **Build**: Maven
- **Tests**: JUnit 5 + Spring Boot Test

---

## 🐳 Running with Docker Compose

```bash
docker-compose up --build
```

This will start:

- PostgreSQL on port **5432**
- Wallet Service on port **8080**

Visit the API docs at  
👉 [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## 🧪 Running Tests

```bash
mvn clean test
```

- Unit tests for domain logic
- Integration tests with Spring Boot

---

## 🧱 Database Migrations

This project uses [Flyway](https://flywaydb.org/) for database versioning.

- Migrations are stored in: `src/main/resources/db/migration`
- First migration: `V1__create_schema.sql`
- Automatically executed at application startup

To manually trigger a migration (if needed):

```bash
mvn flyway:migrate
```

---

## 📬 API Endpoints (Summary)

| Method | Endpoint                             | Description                                          |
|--------|--------------------------------------|------------------------------------------------------|
| POST   | `/users`                             | Create a new user                                    |
| GET    | `/users/{id}`                        | Retrieve a user                                      |
| POST   | `/wallets`                           | Create a new wallet                                  |
| GET    | `/wallets/{id}`                      | Retrieve a wallet                                    |
| POST   | `/wallets/{id}/deposit`              | Deposit funds (`{ "amount": ... }`)                  |
| POST   | `/wallets/{id}/withdraw`             | Withdraw funds (`{ "amount": ... }`)                 |
| POST   | `/wallets/{id}/transfer`             | Transfer funds (`{ "targetWalletId": "...", "amount": ... }`) |
| GET    | `/wallets/{id}/balance`              | Current balance or historical if `?at=` provided      |

---

## 📖 Example Usage

### Create a User

```bash
curl -X POST http://localhost:8080/users \
     -H "Content-Type: application/json" \
     -d '{"name":"Alice"}'
```

### Create a Wallet

```bash
curl -X POST http://localhost:8080/wallets \
     -H "Content-Type: application/json" \
     -d '{"userId":"<USER_UUID>"}'
```

### Deposit Funds

```bash
curl -X POST http://localhost:8080/wallets/<WALLET_UUID>/deposit \
     -H "Content-Type: application/json" \
     -d '{"amount": 50.00}'
```

### Historical Balance

```bash
curl "http://localhost:8080/wallets/<WALLET_UUID>/balance?at=2025-04-25T19:00:00Z"
```

---

## 📁 Project Structure

General folder structure:

```
.
├── docker/
│   └── wait-for-flyway.sh  # Script to help docker compose works (container-init mode)
├── src/
│   ├── main/
│   │   ├── java/           # Main source code
│   │   └── resources/      # Main configs and helper files
│   └── test/                   
│       ├── java/           # Test source code
│       └── resources/      # Test configs and helper files
├── ARD.md                  # Architectural decision records document
├── docker-compose.yaml
├── Dockerfile
├── pom.xml 
└── README.md

```

Main source code:

```
com.rissatto.sws
├── application/
│   ├── mapper/         # Conversão entre DTOs e entidades de domínio
│   └── service/        # Serviços (regras de negócio)
├── domain/             # Entidades de domínio puro
├── infrastructure/
│   ├── config/         # Configurações Spring (Auditing etc.)
│   ├── entity/         # Entidades JPA (persistência)
│   └── repository/     # Repositórios JPA
├── presentation/
│   ├── config/         # Configurações de apresentação (Swagger)
│   ├── controller/     # REST Controllers
│   ├── dto/            # DTOs de entrada e saída
│   └── exception/      # Tratamento de exceções REST
└── SwsApplication.java # Classe principal
```

---

## 🗃️ Notes

- Refer to [ADR.md](ADR.md) for detailed design rationale.
- All timestamps and query‐params are interpreted in UTC.  
