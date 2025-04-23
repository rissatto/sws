package com.rissatto.sws.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record Wallet(UUID id, UUID userId, BigDecimal balance) {

    private static final String MSG_NULL_AMOUNT         = "%s amount must not be null";
    private static final String MSG_NON_POSITIVE_AMOUNT = "%s amount must be positive";
    private static final String MSG_NEGATIVE_BALANCE    = "balance must not be negative";
    private static final String MSG_SAME_WALLET         = "Cannot transfer to the same wallet";
    private static final String MSG_INSUFFICIENT_FUNDS  = "Insufficient funds";

    public Wallet {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(balance, "balance must not be null");
        if (balance.signum() < 0) {
            throw new IllegalArgumentException(MSG_NEGATIVE_BALANCE);
        }
    }

    public static Wallet create(UUID userId) {
        return new Wallet(UUID.randomUUID(), userId, BigDecimal.ZERO);
    }

    private static void checkAmount(BigDecimal amount, String type) {
        Objects.requireNonNull(amount, String.format(MSG_NULL_AMOUNT, type));
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(String.format(MSG_NON_POSITIVE_AMOUNT, type));
        }
    }

    public Wallet deposit(BigDecimal amount) {
        checkAmount(amount, "Deposit");
        return new Wallet(id, userId, balance.add(amount));
    }

    public Wallet withdraw(BigDecimal amount) {
        checkAmount(amount, "Withdraw");
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException(MSG_INSUFFICIENT_FUNDS);
        }
        return new Wallet(id, userId, balance.subtract(amount));
    }

    public TransferResult transferTo(Wallet target, BigDecimal amount) {
        Objects.requireNonNull(target, "target must not be null");
        if (this.id.equals(target.id)) {
            throw new IllegalArgumentException(MSG_SAME_WALLET);
        }
        checkAmount(amount, "Transfer");
        Wallet updatedSource = this.withdraw(amount);
        Wallet updatedDestination = target.deposit(amount);
        return new TransferResult(updatedSource, updatedDestination);
    }

    public record TransferResult(Wallet source, Wallet destination) {
    }

}
