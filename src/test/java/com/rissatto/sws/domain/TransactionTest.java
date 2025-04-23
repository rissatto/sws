package com.rissatto.sws.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    // region ─ creation ───────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldCreateViaConstructorWithValidData() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Transaction.Type type = Transaction.Type.DEPOSIT;
        BigDecimal balance = BigDecimal.ONE;
        Instant now = Instant.now();

        // Act
        Transaction t = new Transaction(id, walletId, type, balance, now);

        // Assert
        assertEquals(id, t.id());
        assertEquals(walletId, t.walletId());
        assertEquals(type, t.type());
        assertEquals(balance, t.amount());
        assertEquals(now, t.timestamp());
    }

    @Test
    void shouldCreateDepositViaFactoryWithValidData() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        Instant before = Instant.now();

        // Act
        Transaction tx = Transaction.createDeposit(walletId, BigDecimal.valueOf(100));
        Instant after = Instant.now();

        // Assert
        assertAll("deposit via factory",
                () -> assertNotNull(tx.id(), "id should be generated"),
                () -> assertEquals(walletId, tx.walletId(), "walletId should match"),
                () -> assertEquals(Transaction.Type.DEPOSIT, tx.type(), "type should be DEPOSIT"),
                () -> assertEquals(BigDecimal.valueOf(100), tx.amount(), "amount should match"),
                () -> assertFalse(tx.timestamp().isBefore(before), "timestamp should be >= factory call"),
                () -> assertFalse(tx.timestamp().isAfter(after), "timestamp should be <= factory return"));
    }

    @Test
    void shouldCreateWithdrawalViaFactoryWithValidData() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        Instant before = Instant.now();

        // Act
        Transaction tx = Transaction.createWithdrawal(walletId, BigDecimal.TEN);
        Instant after = Instant.now();

        // Assert
        assertAll("withdrawal via factory",
                () -> assertNotNull(tx.id(), "id should be generated"),
                () -> assertEquals(walletId, tx.walletId()),
                () -> assertEquals(Transaction.Type.WITHDRAWAL, tx.type()),
                () -> assertEquals(BigDecimal.TEN.negate(), tx.amount()),
                () -> assertFalse(tx.timestamp().isBefore(before)),
                () -> assertFalse(tx.timestamp().isAfter(after)));
    }

    @Test
    void shouldCreateTransferOutViaFactoryWithValidData() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        Instant before = Instant.now();

        // Act
        Transaction tx = Transaction.createTransferOut(walletId, BigDecimal.TEN);
        Instant after = Instant.now();

        // Assert
        assertAll("transfer out via factory",
                () -> assertNotNull(tx.id(), "id should be generated"),
                () -> assertEquals(walletId, tx.walletId()),
                () -> assertEquals(Transaction.Type.TRANSFER_OUT, tx.type()),
                () -> assertEquals(BigDecimal.TEN.negate(), tx.amount()),
                () -> assertFalse(tx.timestamp().isBefore(before)),
                () -> assertFalse(tx.timestamp().isAfter(after)));
    }

    @Test
    void shouldCreateTransferInViaFactoryWithValidData() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        Instant before = Instant.now();

        // Act
        Transaction tx = Transaction.createTransferIn(walletId, BigDecimal.valueOf(75));
        Instant after = Instant.now();

        // Assert
        assertAll("transfer in via factory",
                () -> assertNotNull(tx.id(), "id should be generated"),
                () -> assertEquals(walletId, tx.walletId()),
                () -> assertEquals(Transaction.Type.TRANSFER_IN, tx.type()),
                () -> assertEquals(BigDecimal.valueOf(75), tx.amount()),
                () -> assertFalse(tx.timestamp().isBefore(before)),
                () -> assertFalse(tx.timestamp().isAfter(after)));
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

    // region ─ errors ─────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldThrowViaConstructorWhenIdIsNull() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.TEN;
        Instant now = Instant.now();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new Transaction(null, walletId, Transaction.Type.DEPOSIT, amount, now));

        // Assert
        assertTrue(ex.getMessage().contains("id must not be null"));
    }

    @Test
    void shouldThrowViaConstructorWhenWalletIdIsNull() {
        // Arrange
        UUID id = UUID.randomUUID();
        BigDecimal amount = BigDecimal.TEN;
        Instant now = Instant.now();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new Transaction(id, null, Transaction.Type.DEPOSIT, amount, now));

        // Assert
        assertTrue(ex.getMessage().contains("walletId must not be null"));
    }

    @Test
    void shouldThrowViaFactoryWhenCreatingDepositWithNullWalletId() {
        // Arrange
        BigDecimal amount = BigDecimal.TEN;
        Instant now = Instant.now();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> Transaction.createDeposit(null, amount));

        // Assert
        assertTrue(ex.getMessage().contains("walletId must not be null"));
    }

    @Test
    void shouldThrowViaFactoryWhenCreatingWithdrawalWithNullWalletId() {
        // Arrange
        BigDecimal amount = BigDecimal.TEN;
        Instant now = Instant.now();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> Transaction.createWithdrawal(null, amount));

        // Assert
        assertTrue(ex.getMessage().contains("walletId must not be null"));
    }

    @Test
    void shouldThrowViaFactoryWhenCreatingTransferOutWithNullWalletId() {
        // Arrange
        BigDecimal amount = BigDecimal.TEN;
        Instant now = Instant.now();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> Transaction.createTransferOut(null, amount));

        // Assert
        assertTrue(ex.getMessage().contains("walletId must not be null"));
    }

    @Test
    void shouldThrowViaFactoryWhenCreatingTransferInWithNullWalletId() {
        // Arrange
        BigDecimal amount = BigDecimal.TEN;
        Instant now = Instant.now();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> Transaction.createTransferIn(null, amount));

        // Assert
        assertTrue(ex.getMessage().contains("walletId must not be null"));
    }

    @Test
    void shouldThrowViaConstructorWhenTypeIsNull() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.TEN;
        Instant now = Instant.now();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new Transaction(id, walletId, null, amount, now));

        // Assert
        assertTrue(ex.getMessage().contains("type must not be null"));
    }

    @Test
    void shouldThrowViaConstructorWhenAmountIsNull() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Instant now = Instant.now();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new Transaction(id, walletId, Transaction.Type.DEPOSIT, null, now));

        // Assert
        assertTrue(ex.getMessage().contains("amount must not be null"));
    }

    @Test
    void shouldThrowViaFactoryWhenCreatingDepositWithNullAmount() {
        // Arrange
        UUID walletId = UUID.randomUUID();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> Transaction.createDeposit(walletId, null));

        // Assert
        assertTrue(ex.getMessage().contains("amount must not be null"));
    }

    @Test
    void shouldThrowViaFactoryWhenCreatingWithdrawalWithNullAmount() {
        // Arrange
        UUID walletId = UUID.randomUUID();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> Transaction.createWithdrawal(walletId, null));

        // Assert
        assertTrue(ex.getMessage().contains("amount must not be null"));
    }

    @Test
    void shouldThrowViaFactoryWhenCreatingTransferOutWithNullAmount() {
        // Arrange
        UUID walletId = UUID.randomUUID();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> Transaction.createTransferOut(walletId, null));

        // Assert
        assertTrue(ex.getMessage().contains("amount must not be null"));
    }

    @Test
    void shouldThrowViaFactoryWhenCreatingTransferInWithNullAmount() {
        // Arrange
        UUID walletId = UUID.randomUUID();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> Transaction.createTransferIn(walletId, null));

        // Assert
        assertTrue(ex.getMessage().contains("amount must not be null"));
    }

    @Test
    void shouldThrowViaConstructorWhenAmountIsZero() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Instant now = Instant.now();

        // Act
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Transaction(id, walletId, Transaction.Type.WITHDRAWAL, BigDecimal.ZERO, now));

        // Assert
        assertTrue(ex.getMessage().contains("amount must be positive or negative"));
    }

    @Test
    void shouldThrowViaConstructorWhenTimestampIsNull() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.TEN;

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new Transaction(id, walletId, Transaction.Type.DEPOSIT, amount, null));

        // Assert
        assertTrue(ex.getMessage().contains("timestamp must not be null"));
    }

    @Test
    void shouldThrowViaConstructorWhenTimestampInFuture() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.ONE;
        Instant future = Instant.now().plusSeconds(60);

        // Act
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Transaction(id, walletId, Transaction.Type.DEPOSIT, amount, future));

        // Assert
        assertTrue(ex.getMessage().contains("timestamp cannot be in the future"));
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────
}
