package com.rissatto.sws.infrastructure.repository;

import com.rissatto.sws.domain.Transaction;
import com.rissatto.sws.infrastructure.entity.TransactionEntity;
import com.rissatto.sws.infrastructure.entity.UserEntity;
import com.rissatto.sws.infrastructure.entity.WalletEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionRepositoryIT extends AbstractRepositoryIT {

    private static final Logger log = LoggerFactory.getLogger(TransactionRepositoryIT.class);

    private static final AtomicInteger USER_COUNTER = new AtomicInteger();
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository repository;
    private final PlatformTransactionManager transactionManager;

    private UUID globalWalletId;

    @Autowired
    public TransactionRepositoryIT(UserRepository userRepository, WalletRepository walletRepository, TransactionRepository repository, PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.repository = repository;
        this.transactionManager = transactionManager;
    }

    @BeforeEach
    void beforeEach() {
        String userName = "Richard Roe " + USER_COUNTER.incrementAndGet();
        globalWalletId = runInTransaction(() -> {
            UserEntity user = new UserEntity(userName);
            userRepository.save(user);
            WalletEntity wallet = new WalletEntity(user.getId(), BigDecimal.ZERO);
            walletRepository.save(wallet);
            return wallet.getId();
        });
    }

    private <T> T runInTransaction(Supplier<T> logic) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx.execute(status -> logic.get());
    }

    @Test
    void shouldSaveAndFindByWalletId() {
        TransactionEntity tx = new TransactionEntity(
                UUID.randomUUID(),
                globalWalletId,
                Transaction.Type.DEPOSIT,
                BigDecimal.TEN,
                Instant.now()
        );

        repository.save(tx);

        List<TransactionEntity> results = repository.findByWalletId(globalWalletId);

        assertThat(results)
                .hasSize(1)
                .first()
                .satisfies(txFound -> {
                    assertThat(txFound.getAmount()).isEqualTo(BigDecimal.TEN);
                    assertThat(txFound.getType()).isEqualTo(Transaction.Type.DEPOSIT);
                    assertThat(txFound.getTimestamp()).isNotNull();
                    log.info("\n🎯 Transaction found in database by walletId:\n{}", txFound);
                });
    }

    @Test
    void findByWalletIdAndTimestampLessThanEqualShouldReturnTransactionsBeforeOrAtGivenTime() {
        // Arrange
        Instant instant = Instant.now();
        Instant before = instant.minusSeconds(60);
        Instant after = instant.plusSeconds(60);

        TransactionEntity tx1 = new TransactionEntity(globalWalletId, Transaction.Type.DEPOSIT, BigDecimal.TEN, before);
        TransactionEntity tx2 = new TransactionEntity(globalWalletId, Transaction.Type.WITHDRAWAL, BigDecimal.ONE, instant);
        TransactionEntity tx3 = new TransactionEntity(globalWalletId, Transaction.Type.DEPOSIT, BigDecimal.TEN, after);

        runInTransaction(() -> {
            repository.saveAll(List.of(tx1, tx2, tx3));
            return null;
        });

        // Act
        List<TransactionEntity> result = repository.findByWalletIdAndTimestampLessThanEqual(globalWalletId, instant);

        // Assert
        assertThat(result)
                .as("Should only include transactions with timestamp ≤ the given instant")
                .hasSize(2)
                .extracting(TransactionEntity::getId)
                .containsExactlyInAnyOrder(tx1.getId(), tx2.getId());
    }
}
