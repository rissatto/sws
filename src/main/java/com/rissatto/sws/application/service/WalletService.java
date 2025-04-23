package com.rissatto.sws.application.service;

import com.rissatto.sws.domain.Wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface WalletService {

    Wallet create(UUID userId);

    Wallet create(UUID userId, String idempotencyKey);

    Wallet getById(UUID walletId);

    BigDecimal getCurrentBalance(UUID walletId);

    Wallet deposit(UUID walletId, BigDecimal amount);

    Wallet deposit(UUID walletId, BigDecimal amount, String idempotencyKey);

    Wallet withdraw(UUID walletId, BigDecimal amount);

    Wallet withdraw(UUID walletId, BigDecimal amount, String idempotencyKey);

    Wallet transfer(UUID sourceWalletId, UUID targetWalletId, BigDecimal amount);

    Wallet transfer(UUID sourceWalletId, UUID targetWalletId, BigDecimal amount, String idempotencyKey);

    BigDecimal getHistoricalBalance(UUID walletId, Instant at);
}