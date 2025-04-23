package com.rissatto.sws.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Transaction(UUID id, UUID walletId, Type type, BigDecimal amount, Instant timestamp) {
    public Transaction {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");

        if (amount.signum() == 0) {
            throw new IllegalArgumentException("amount must be positive or negative");
        }
        if (timestamp.isAfter(Instant.now())) {
            throw new IllegalArgumentException("timestamp cannot be in the future");
        }
    }

    public static Transaction createDeposit(UUID walletId, BigDecimal amount) {
        return new Transaction(
                UUID.randomUUID(),
                walletId,
                Type.DEPOSIT,
                amount == null ? null : amount.abs(),
                Instant.now());
    }

    public static Transaction createWithdrawal(UUID walletId, BigDecimal amount) {
        return new Transaction(UUID.randomUUID(),
                walletId,
                Type.WITHDRAWAL,
                amount == null ? null : amount.abs().negate(),
                Instant.now());
    }

    public static Transaction createTransferOut(UUID walletId, BigDecimal amount) {
        return new Transaction(
                UUID.randomUUID(),
                walletId,
                Type.TRANSFER_OUT,
                amount == null ? null : amount.abs().negate(),
                Instant.now());
    }

    public static Transaction createTransferIn(UUID walletId, BigDecimal amount) {
        return new Transaction(
                UUID.randomUUID(),
                walletId,
                Type.TRANSFER_IN,
                amount == null ? null : amount.abs(),
                Instant.now());
    }

    public enum Type {
        DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT;

        public boolean isDeposit() {
            return this == DEPOSIT;
        }

        public boolean isWithdrawal() {
            return this == WITHDRAWAL;
        }

        public boolean isTransferOut() {
            return this == TRANSFER_OUT;
        }

        public boolean isTransferIn() {
            return this == TRANSFER_IN;
        }

        public boolean isTransfer() {
            return this == TRANSFER_IN || this == TRANSFER_OUT;
        }

        public boolean isDebit() {
            return this == WITHDRAWAL || this == TRANSFER_OUT;
        }

        public boolean isCredit() {
            return this == DEPOSIT || this == TRANSFER_IN;
        }
    }

}