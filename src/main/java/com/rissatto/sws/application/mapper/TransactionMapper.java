package com.rissatto.sws.application.mapper;

import com.rissatto.sws.domain.Transaction;
import com.rissatto.sws.infrastructure.entity.TransactionEntity;

public final class TransactionMapper {

    private TransactionMapper() {
        // Utility class
    }

    public static TransactionEntity toEntity(Transaction transaction) {
        return new TransactionEntity(
                transaction.id(),
                transaction.walletId(),
                transaction.type(),
                transaction.amount(),
                transaction.timestamp()
        );
    }

    public static Transaction toDomain(TransactionEntity entity) {
        return new Transaction(
                entity.getId(),
                entity.getWalletId(),
                entity.getType(),
                entity.getAmount(),
                entity.getTimestamp()
        );
    }
}
