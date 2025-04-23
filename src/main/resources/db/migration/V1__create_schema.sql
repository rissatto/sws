-- V1__create_schema.sql

CREATE TABLE users (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    balance NUMERIC NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount NUMERIC NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    operation VARCHAR(50) NOT NULL,
    resource_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

ALTER TABLE wallets
  ADD CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE transactions
  ADD CONSTRAINT fk_tx_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id);
