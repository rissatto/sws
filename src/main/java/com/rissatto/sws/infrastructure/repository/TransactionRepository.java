package com.rissatto.sws.infrastructure.repository;

import com.rissatto.sws.infrastructure.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findByWalletId(UUID walletId);

    List<TransactionEntity> findByWalletIdIn(Collection<UUID> walletIds);

    List<TransactionEntity> findByWalletIdAndTimestampLessThanEqual(UUID walletId, Instant timestamp);
}
