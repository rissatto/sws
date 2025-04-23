package com.rissatto.sws.infrastructure.repository;

import com.rissatto.sws.infrastructure.entity.UserEntity;
import com.rissatto.sws.infrastructure.entity.WalletEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class WalletRepositoryIT extends AbstractRepositoryIT {

    private static final AtomicInteger USER_COUNTER = new AtomicInteger();
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PlatformTransactionManager transactionManager;
    private ExecutorService executor;
    private UUID globalUserId;

    @Autowired
    public WalletRepositoryIT(UserRepository userRepository,
                              WalletRepository walletRepository,
                              PlatformTransactionManager platformTransactionManager) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionManager = platformTransactionManager;
    }

    @BeforeEach
    void beforeEach() {
        String userName = "John Doe " + USER_COUNTER.incrementAndGet();
        globalUserId = runInTransaction(() -> {
            UserEntity user = new UserEntity(userName);
            userRepository.save(user);
            return user.getId();
        });
    }

    @AfterEach
    void tearDownExecutor() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    private UUID createWalletWithBalance(BigDecimal balance) {
        WalletEntity wallet = new WalletEntity(globalUserId, balance);
        walletRepository.save(wallet);
        return wallet.getId();
    }

    @Test
    void shouldSaveAndFindWalletByIdWithAuditData() {
        // Arrange & Act
        WalletEntity wallet = new WalletEntity(globalUserId, BigDecimal.ZERO);
        walletRepository.save(wallet);
        Optional<WalletEntity> walletFound = walletRepository.findById(wallet.getId());

        // Assert
        assertThat(walletFound).isPresent()
                .get()
                .satisfies(db -> {
                    assertThat(db.getBalance()).isEqualTo(BigDecimal.ZERO);
                    assertThat(db.getCreatedAt()).isNotNull();
                    assertThat(db.getUpdatedAt()).isNotNull();
                });
    }

    @Test
    void concurrentWithdrawalsWithLockShouldResultInCorrectBalance() throws ExecutionException, InterruptedException {
        // Arrange
        executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        UUID walletId = runInTransaction(() -> createWalletWithBalance(BigDecimal.TWO));

        // Act
        Future<Void> thread1 = executor.submit(() -> // Thread 1: acquires lock, performs withdrawal, commits
                runInTransaction(() -> {
                    // lock the row
                    WalletEntity w1 = walletRepository.findByIdForUpdate(walletId)
                            .orElseThrow();
                    latch.countDown();  // signal thread2 to start
                    w1.setBalance(w1.getBalance().subtract(BigDecimal.ONE));
                    walletRepository.save(w1);
                    return null;
                })
        );

        Future<Void> thread2 = executor.submit(() -> { // Thread 2: waits for lockAcquired, then tries to lock & withdraw
            latch.await();  // wait until thread1 holds the lock
            return runInTransaction(() -> {
                WalletEntity w2 = walletRepository.findByIdForUpdate(walletId)
                        .orElseThrow();
                w2.setBalance(w2.getBalance().subtract(BigDecimal.ONE));
                walletRepository.save(w2);
                return null;
            });
        });

        // Just for fire threads
        thread1.get();
        thread2.get();

        BigDecimal finalBalance = walletRepository.findById(walletId).orElseThrow().getBalance();

        // Assert
        assertThat(finalBalance)
                .as("With lock, two concurrent withdrawals of 1 from a 2 balance must leave exactly 0")
                .isZero();
    }

    @Test
    void concurrentWithdrawalsWithoutLockShouldResultInIncorrectBalance() throws ExecutionException, InterruptedException {
        // Arrange
        executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        UUID walletId = runInTransaction(() -> createWalletWithBalance(BigDecimal.TWO));

        // Act
        Future<Void> thread1 = executor.submit(() -> // Thread1: reads balance, signals thread2, withdrawal
                runInTransaction(() -> {
                    WalletEntity w1 = walletRepository.findById(walletId).orElseThrow();
                    latch.countDown();  // let thread2 proceed
                    w1.setBalance(w1.getBalance().subtract(BigDecimal.ONE));
                    walletRepository.save(w1);
                    return null;
                })
        );

        Future<Void> thread2 = executor.submit(() -> { // Thread2: waits latch, reads same balance, withdrawal (overdraw)
            latch.await();
            return runInTransaction(() -> {
                WalletEntity w2 = walletRepository.findById(walletId).orElseThrow();
                w2.setBalance(w2.getBalance().subtract(BigDecimal.ONE));
                walletRepository.save(w2);
                return null;
            });
        });

        // Just for fire threads
        thread1.get();
        thread2.get();

        BigDecimal finalBalance = walletRepository.findById(walletId).orElseThrow().getBalance();

        // Assert
        assertThat(finalBalance)
                .as("Without lock, two concurrent withdrawals of 1 from a 2 balance must leave wrong 1 balance")
                .isOne();
    }

    private <T> T runInTransaction(Supplier<T> logic) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx.execute(status -> logic.get());
    }

}
