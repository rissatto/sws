package com.rissatto.sws.application.service;

import com.rissatto.sws.domain.Transaction;
import com.rissatto.sws.domain.Wallet;
import com.rissatto.sws.infrastructure.entity.TransactionEntity;
import com.rissatto.sws.infrastructure.entity.UserEntity;
import com.rissatto.sws.infrastructure.entity.WalletEntity;
import com.rissatto.sws.infrastructure.repository.IdempotencyKeyRepository;
import com.rissatto.sws.infrastructure.repository.TransactionRepository;
import com.rissatto.sws.infrastructure.repository.UserRepository;
import com.rissatto.sws.infrastructure.repository.WalletRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.BIG_DECIMAL;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WalletServiceIT {

    private static final String CREATE_OPERATION = "createWallet";
    private static final String DEPOSIT_OPERATION = "depositWallet";
    private static final String WITHDRAW_OPERATION = "withdrawWallet";
    private static final String TRANSFER_OPERATION = "transferWallet";

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private UUID globalUserId;

    @BeforeEach
    void beforeEach() {
        if (globalUserId == null) {
            UserEntity user = new UserEntity("John Roe");
            userRepository.save(user);
            globalUserId = user.getId();
        }
    }

    // region ─ create ─────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldCreate() {
        // Act
        Wallet created = walletService.create(globalUserId);

        // Assert
        assertThat(created).isNotNull();
        assertThat(walletRepository.findById(created.id())).isPresent();
    }

    @Test
    void shouldCreateWithIdempotencyKey() {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();

        // Act
        Wallet created = walletService.create(globalUserId, idempotencyKey);

        // Assert
        assertThat(created).isNotNull();
        assertThat(walletRepository.findById(created.id())).isPresent();
        assertThat(idempotencyKeyRepository.findByIdempotencyKeyAndOperation(idempotencyKey, CREATE_OPERATION)).isPresent();
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

    // region ─ getById ────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldGetById() {
        // Arrange
        Wallet created = walletService.create(globalUserId);

        // Act
        Wallet found = walletService.getById(created.id());

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.userId()).isEqualTo(globalUserId);
        assertThat(found.balance()).isEqualByComparingTo(created.balance());
    }

    @Test
    void shouldThrowWhenGetByIdNotFound() {
        // Arrange
        UUID missing = UUID.randomUUID();

        // Act & Assert
        assertThatThrownBy(() -> walletService.getById(missing))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Wallet not found");
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

    // region ─ getCurrentBalance ──────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldGetCurrentBalance() {
        // Arrange
        Wallet w = walletService.create(globalUserId);
        BigDecimal deposit = BigDecimal.valueOf(123.45);
        walletService.deposit(w.id(), deposit);

        // Act
        BigDecimal balance = walletService.getCurrentBalance(w.id());

        // Assert
        assertThat(balance).isEqualByComparingTo(deposit);
    }

    @Test
    void shouldThrowWhenGetCurrentBalanceNotFound() {
        // Arrange
        UUID missing = UUID.randomUUID();

        // Act & Assert
        assertThatThrownBy(() -> walletService.getCurrentBalance(missing))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Wallet not found");
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

    // region ─ operations ─────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldDeposit() {
        // Arrange
        Wallet w = walletService.create(globalUserId);
        BigDecimal amount = BigDecimal.ONE;

        // Act
        Wallet after = walletService.deposit(w.id(), amount);

        // Assert
        assertThat(after.balance()).isEqualByComparingTo(amount);
        assertThat(walletRepository.findById(w.id()))
                .isPresent()
                .get().extracting(WalletEntity::getBalance, BIG_DECIMAL).isEqualByComparingTo(amount);

        List<TransactionEntity> txs = transactionRepository.findByWalletId(w.id());
        assertThat(txs).hasSize(1);
        assertThat(txs.getFirst().getType()).isEqualTo(Transaction.Type.DEPOSIT);
        assertThat(txs.getFirst().getAmount()).isEqualByComparingTo(amount);
    }

    @Test
    void shouldDepositIdempotent() {
        // Arrange
        Wallet w = walletService.create(globalUserId);
        BigDecimal amount = BigDecimal.ONE;
        String idempotencyKey = UUID.randomUUID().toString();

        // Act
        Wallet first = walletService.deposit(w.id(), amount, idempotencyKey);
        Wallet second = walletService.deposit(w.id(), amount, idempotencyKey);

        // Assert
        assertThat(first.balance()).isEqualByComparingTo(amount);
        assertThat(second.balance()).isEqualByComparingTo(amount);
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(idempotencyKeyRepository
                .findByIdempotencyKeyAndOperation(idempotencyKey, DEPOSIT_OPERATION))
                .isPresent();
    }

    @Test
    void shouldWithdraw() {
        // Arrange
        Wallet w = walletService.create(globalUserId);
        walletService.deposit(w.id(), BigDecimal.TWO);
        BigDecimal withdraw = BigDecimal.ONE;
        BigDecimal expectedBalance = BigDecimal.ONE;

        // Act
        Wallet after = walletService.withdraw(w.id(), withdraw);

        // Assert
        assertThat(after.balance()).isEqualByComparingTo(expectedBalance);
        assertThat(walletRepository.findById(w.id()))
                .isPresent()
                .get().extracting(WalletEntity::getBalance, BIG_DECIMAL).isEqualByComparingTo(expectedBalance);

        List<TransactionEntity> txs = transactionRepository.findByWalletId(w.id());
        assertThat(txs).hasSize(2);
        TransactionEntity wtx = txs.stream()
                .filter(t -> t.getType().isWithdrawal()).findFirst().orElseThrow();
        assertThat(wtx.getAmount()).isEqualByComparingTo(withdraw.negate());
    }

    @Test
    void shouldWithdrawIdempotent() {
        // Arrange
        String key = UUID.randomUUID().toString();
        Wallet w = walletService.create(globalUserId);
        walletService.deposit(w.id(), BigDecimal.TWO);
        BigDecimal withdraw = BigDecimal.ONE;
        BigDecimal expectedBalance = BigDecimal.ONE;

        // Act
        Wallet first = walletService.withdraw(w.id(), withdraw, key);
        Wallet second = walletService.withdraw(w.id(), withdraw, key);

        assertThat(first.balance()).isEqualByComparingTo(expectedBalance);
        assertThat(second.balance()).isEqualByComparingTo(expectedBalance);
        assertThat(transactionRepository.findByWalletId(w.id()).stream()
                .filter(t -> t.getType().isWithdrawal()).count())
                .isEqualTo(1);
        assertThat(idempotencyKeyRepository
                .findByIdempotencyKeyAndOperation(key, WITHDRAW_OPERATION))
                .isPresent();
    }

    @Test
    void shouldFailWithdrawWhenInsufficient() {
        // Arrange
        Wallet w = walletService.create(globalUserId);

        // Act & Assert
        assertThatThrownBy(() -> walletService.withdraw(w.id(), BigDecimal.TEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    void shouldTransfer() {
        // Arrange
        Wallet w1 = walletService.create(globalUserId);
        Wallet w2 = walletService.create(globalUserId);
        walletService.deposit(w1.id(), BigDecimal.ONE);

        // Act
        Wallet after = walletService.transfer(w1.id(), w2.id(), BigDecimal.ONE);

        // Assert
        assertThat(after.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(walletRepository.findById(w1.id()))
                .isPresent()
                .get().extracting(WalletEntity::getBalance, BIG_DECIMAL).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(walletRepository.findById(w2.id()))
                .isPresent()
                .get().extracting(WalletEntity::getBalance, BIG_DECIMAL).isEqualByComparingTo(BigDecimal.ONE);

        List<TransactionEntity> txs = transactionRepository.findByWalletIdIn(List.of(w1.id(), w2.id()));
        assertThat(txs).hasSize(3);
        assertThat(txs.stream().filter(t -> t.getType().isTransferOut()).count()).isEqualTo(1);
        assertThat(txs.stream().filter(t -> t.getType().isTransferIn()).count()).isEqualTo(1);
    }

    @Test
    void shouldTransferIdempotent() {
        Wallet w1 = walletService.create(globalUserId);
        Wallet w2 = walletService.create(globalUserId);
        walletService.deposit(w1.id(), BigDecimal.TWO);

        String key = UUID.randomUUID().toString();
        Wallet first = walletService.transfer(w1.id(), w2.id(), BigDecimal.ONE, key);
        Wallet second = walletService.transfer(w1.id(), w2.id(), BigDecimal.ONE, key);

        assertThat(first.balance()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(second.balance()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(transactionRepository.findByWalletIdIn(List.of(w1.id(), w2.id())).stream()
                .filter(t -> t.getType().isTransfer()).count()).isEqualTo(2);
        assertThat(idempotencyKeyRepository
                .findByIdempotencyKeyAndOperation(key, TRANSFER_OPERATION))
                .isPresent();
    }

    @Test
    void shouldFailTransferWhenWalletNotFound() {
        Wallet w1 = walletService.create(globalUserId);
        UUID bad = UUID.randomUUID();
        assertThatThrownBy(() -> walletService.transfer(bad, w1.id(), BigDecimal.ONE))
                .isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> walletService.transfer(w1.id(), bad, BigDecimal.ONE))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

}
