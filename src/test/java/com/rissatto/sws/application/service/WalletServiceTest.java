package com.rissatto.sws.application.service;

import com.rissatto.sws.domain.Transaction;
import com.rissatto.sws.domain.Wallet;
import com.rissatto.sws.infrastructure.entity.IdempotencyKey;
import com.rissatto.sws.infrastructure.entity.TransactionEntity;
import com.rissatto.sws.infrastructure.entity.WalletEntity;
import com.rissatto.sws.infrastructure.repository.IdempotencyKeyRepository;
import com.rissatto.sws.infrastructure.repository.TransactionRepository;
import com.rissatto.sws.infrastructure.repository.WalletRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    // region ─ create ─────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldCreate() {
        // Arrange
        UUID existingUserId = UUID.randomUUID();
        when(walletRepository.save(any(WalletEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Wallet createdWallet = walletService.create(existingUserId);

        // Assert
        verify(walletRepository).save(any());
        assertThat(createdWallet).isNotNull();
        assertThat(createdWallet.id()).isNotNull();
        assertThat(createdWallet.userId()).isEqualTo(existingUserId);
        assertThat(createdWallet.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCreateForUserWithIdempotencyKey() {
        // Arrange
        UUID existingUserId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        when(walletRepository.save(any(WalletEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Wallet createdWallet = walletService.create(existingUserId, idempotencyKey);

        // Assert
        verify(walletRepository).save(any(WalletEntity.class));
        verify(idempotencyKeyRepository).save(any(IdempotencyKey.class));
        assertThat(createdWallet).isNotNull();
        assertThat(createdWallet.id()).isNotNull();
        assertThat(createdWallet.userId()).isEqualTo(existingUserId);
        assertThat(createdWallet.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

    // region ─ getById ────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldGetById() {
        // Arrange
        UUID existingId = UUID.randomUUID();
        UUID existingUserId = UUID.randomUUID();
        WalletEntity w = new WalletEntity(existingUserId, BigDecimal.ZERO);
        w.setId(existingId);
        when(walletRepository.findById(existingId)).thenReturn(Optional.of(w));

        // Act
        Wallet foundWallet = walletService.getById(existingId);

        // Assert
        assertThat(foundWallet).isNotNull();
        assertThat(foundWallet.id()).isEqualTo(existingId);
        assertThat(foundWallet.userId()).isEqualTo(existingUserId);
        assertThat(foundWallet.balance()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void shouldThrowExceptionWhenWalletNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(walletRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> walletService.getById(nonExistentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Wallet not found");
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

    // region ─ getCurrentBalance ──────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldReturnCurrentBalance() {
        // Arrange
        UUID existingWalletId = UUID.randomUUID();
        UUID existingUserId = UUID.randomUUID();
        WalletEntity w = new WalletEntity(existingUserId, BigDecimal.TEN);
        w.setId(existingWalletId);
        when(walletRepository.findById(existingWalletId)).thenReturn(Optional.of(w));

        // Act
        BigDecimal balance = walletService.getCurrentBalance(existingWalletId);

        // Assert
        assertThat(balance).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void shouldThrowWhenGetCurrentBalanceNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(walletRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> walletService.getCurrentBalance(nonExistentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Wallet not found");
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

    // region ─ operations ─────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldDepositAndSaveTransaction() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.ONE;

        WalletEntity entity = new WalletEntity(userId, BigDecimal.ZERO);
        entity.setId(walletId);
        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(entity));
        when(walletRepository.save(any(WalletEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);

        // Act
        Wallet result = walletService.deposit(walletId, amount);

        // Assert
        verify(walletRepository).findByIdForUpdate(walletId);
        verify(walletRepository).save(any(WalletEntity.class));
        verify(transactionRepository).save(captor.capture());

        TransactionEntity savedTransaction = captor.getValue();

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(walletId);
        assertThat(result.balance()).isEqualByComparingTo(amount);

        assertThat(savedTransaction.getWalletId()).isEqualTo(walletId);
        assertThat(savedTransaction.getAmount()).isEqualByComparingTo(amount);
        assertThat(savedTransaction.getType()).isEqualTo(Transaction.Type.DEPOSIT);
        assertThat(savedTransaction.getTimestamp()).isNotNull();
    }

    @Test
    void shouldWithdrawAndSaveTransaction() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal initialBalance = BigDecimal.ONE;
        BigDecimal withdrawAmount = BigDecimal.ONE;

        WalletEntity entity = new WalletEntity(userId, initialBalance);
        entity.setId(walletId);
        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(entity));
        when(walletRepository.save(any(WalletEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);

        // Act
        Wallet result = walletService.withdraw(walletId, withdrawAmount);

        // Assert
        verify(walletRepository).findByIdForUpdate(walletId);
        verify(walletRepository).save(any(WalletEntity.class));
        verify(transactionRepository).save(captor.capture());

        TransactionEntity savedTransaction = captor.getValue();

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(walletId);
        assertThat(result.balance()).isEqualByComparingTo(initialBalance.subtract(withdrawAmount));

        assertThat(savedTransaction.getWalletId()).isEqualTo(walletId);
        assertThat(savedTransaction.getAmount()).isEqualByComparingTo(withdrawAmount.negate());
        assertThat(savedTransaction.getType()).isEqualTo(Transaction.Type.WITHDRAWAL);
        assertThat(savedTransaction.getTimestamp()).isNotNull();
    }

    @Test
    void shouldTransferAndSaveTransaction() {
        // Arrange
        UUID sourceWalletId = UUID.randomUUID();
        UUID targetWalletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal initialBalanceSource = BigDecimal.ONE;
        BigDecimal initialBalanceTarget = BigDecimal.ZERO;
        BigDecimal transferAmount = BigDecimal.ONE;

        WalletEntity sourceEntity = new WalletEntity(userId, initialBalanceSource);
        sourceEntity.setId(sourceWalletId);

        WalletEntity targetEntity = new WalletEntity(userId, initialBalanceTarget);
        targetEntity.setId(targetWalletId);

        when(walletRepository.findByIdForUpdate(sourceWalletId)).thenReturn(Optional.of(sourceEntity));
        when(walletRepository.findByIdForUpdate(targetWalletId)).thenReturn(Optional.of(targetEntity));
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);

        // Act
        Wallet result = walletService.transfer(sourceWalletId, targetWalletId, transferAmount);

        // Assert
        verify(walletRepository).findByIdForUpdate(sourceWalletId);
        verify(walletRepository).findByIdForUpdate(targetWalletId);
        verify(walletRepository, times(2)).save(any(WalletEntity.class));
        verify(transactionRepository, times(2)).save(captor.capture());

        var transactions = captor.getAllValues();
        assertThat(transactions).hasSize(2);

        TransactionEntity sourceTransaction = transactions.get(0);
        TransactionEntity targetTransaction = transactions.get(1);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(sourceWalletId);
        assertThat(result.balance()).isEqualByComparingTo(initialBalanceSource.subtract(transferAmount));

        assertThat(sourceTransaction.getWalletId()).isEqualTo(sourceWalletId);
        assertThat(sourceTransaction.getAmount()).isEqualByComparingTo(transferAmount.negate());
        assertThat(sourceTransaction.getType()).isEqualTo(Transaction.Type.TRANSFER_OUT);

        assertThat(targetTransaction.getWalletId()).isEqualTo(targetWalletId);
        assertThat(targetTransaction.getAmount()).isEqualByComparingTo(transferAmount);
        assertThat(targetTransaction.getType()).isEqualTo(Transaction.Type.TRANSFER_IN);
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────
}
