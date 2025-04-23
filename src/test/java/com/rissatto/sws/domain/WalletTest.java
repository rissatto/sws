package com.rissatto.sws.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class WalletTest {

    private static Wallet createWallet() {
        UUID userId = UUID.randomUUID();
        return Wallet.create(userId);
    }

    // region ─ creation ───────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldCreateViaConstructorWithValidIdAndUserIdAndBalance() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal balance = BigDecimal.ZERO;

        // Act
        Wallet w = new Wallet(id, userId, balance);

        // Assert
        assertEquals(id, w.id());
        assertEquals(userId, w.userId());
        assertEquals(balance, w.balance());
    }

    @Test
    void shouldCreateViaFactorySettingZeroBalanceWhenValidUserId() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        Wallet w = Wallet.create(userId);

        // Assert
        assertNotNull(w.id());
        assertEquals(userId, w.userId());
        assertEquals(BigDecimal.ZERO, w.balance());
    }

    @Test
    void shouldThrowViaConstructorWhenIdIsNull() {
        // Arrange
        UUID userId = UUID.randomUUID();
        BigDecimal balance = BigDecimal.ZERO;

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new Wallet(null, userId, balance),
                "constructing with null id should throw NullPointerException");

        // Assert
        assertThat(ex.getMessage()).contains("id must not be null");
    }

    @Test
    void shouldThrowViaConstructorWhenUserIdIsNull() {
        // Arrange
        UUID id = UUID.randomUUID();
        BigDecimal balance = BigDecimal.ZERO;

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new Wallet(id, null, balance),
                "constructing with null userId should throw NullPointerException");

        // Assert
        assertThat(ex.getMessage()).contains("userId must not be null");
    }

    @Test
    void shouldThrowWhenCreateViaFactoryWithNullUserId() {
        // Act
        NullPointerException ex = assertThrows(NullPointerException.class, () -> Wallet.create(null));

        // Assert
        assertThat(ex.getMessage()).contains("userId must not be null");
    }

    @Test
    void shouldThrowViaConstructorWhenBalanceIsNull() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new Wallet(id, userId, null),
                "constructing with null balance should throw NullPointerException");

        // Assert
        assertThat(ex.getMessage()).contains("balance must not be null");
    }

    @Test
    void shouldThrowViaConstructorWhenBalanceIsNegative() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Act
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Wallet(id, userId, BigDecimal.ONE.negate()),
                "constructing with negative balance should throw IAE");

        // Assert
        assertThat(ex.getMessage()).contains("balance must not be negative");
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

    // region ─ domain behavior ────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldDepositPositiveAmount() {
        // Arrange
        Wallet w1 = createWallet();

        // Act
        Wallet w2 = w1.deposit(BigDecimal.ONE);

        // Assert
        assertAll("deposit",
                () -> assertEquals(BigDecimal.ZERO, w1.balance(), "original wallet must remain unchanged"),
                () -> assertEquals(BigDecimal.ONE, w2.balance(), "new wallet must reflect deposit"));
    }

    @Test
    void shouldThrowWhenDepositingNullAmount() {
        // Arrange
        Wallet w = createWallet();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> w.deposit(null),
                "depositing null should throw NPE");

        // Assert
        assertThat(ex.getMessage()).contains("Deposit amount must not be null");
    }

    @Test
    void shouldThrowWhenDepositingNonPositiveAmount() {
        // Arrange
        Wallet w = createWallet();

        // Act
        IllegalArgumentException exZero = assertThrows(
                IllegalArgumentException.class,
                () -> w.deposit(BigDecimal.ZERO),
                "depositing zero should throw IAE");
        IllegalArgumentException exNegative = assertThrows(
                IllegalArgumentException.class,
                () -> w.deposit(BigDecimal.ONE.negate()),
                "depositing negative should throw IAE");

        // Assert
        assertThat(exZero.getMessage()).contains("Deposit amount must be positive");
        assertThat(exNegative.getMessage()).contains("Deposit amount must be positive");
    }

    @Test
    void shouldWithdrawPositiveAmount() {
        // Arrange
        Wallet w1 = createWallet().deposit(BigDecimal.TWO);

        // Act
        Wallet w2 = w1.withdraw(BigDecimal.ONE);

        // Assert
        assertAll("withdraw",
                () -> assertEquals(BigDecimal.TWO, w1.balance(), "original wallet must remain unchanged"),
                () -> assertEquals(BigDecimal.ONE, w2.balance(), "new wallet must reflect withdrawal"));
    }

    @Test
    void shouldThrowWhenWithdrawingNullAmount() {
        // Arrange
        Wallet w = createWallet();

        // Act
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> w.withdraw(null),
                "withdrawing null should throw NPE");

        // Assert
        assertThat(ex.getMessage()).contains("amount must not be null");
    }

    @Test
    void shouldThrowWhenWithdrawingNonPositiveAmount() {
        // Arrange
        Wallet w = createWallet();

        // Act
        IllegalArgumentException exZero = assertThrows(
                IllegalArgumentException.class,
                () -> w.withdraw(BigDecimal.ZERO),
                "withdrawing zero should throw IAE");
        IllegalArgumentException exNegative = assertThrows(
                IllegalArgumentException.class,
                () -> w.withdraw(BigDecimal.ONE.negate()),
                "withdrawing negative should throw IAE");

        // Assert
        assertThat(exZero.getMessage()).contains("Withdraw amount must be positive");
        assertThat(exNegative.getMessage()).contains("Withdraw amount must be positive");
    }

    @Test
    void shouldThrowWhenWithdrawingMoreThanBalance() {
        // Arrange
        Wallet w = createWallet();

        // Act
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> w.withdraw(BigDecimal.TEN),
                "withdrawing more than balance should throw IAE");

        // Assert
        assertThat(ex.getMessage()).contains("Insufficient funds");
    }

    @Test
    void shouldTransferToAnotherWallet() {
        // Arrange
        Wallet source = createWallet().deposit(BigDecimal.TWO);
        Wallet destination = createWallet();

        // Act
        Wallet.TransferResult result = source.transferTo(destination, BigDecimal.ONE);

        // Assert
        assertAll("immutability",
                () -> assertEquals(BigDecimal.TWO, source.balance(), "original source wallet must remain unchanged"),
                () -> assertEquals(BigDecimal.ZERO, destination.balance(), "original destination wallet must remain unchanged")
        );
        assertAll("transfer",
                () -> assertEquals(BigDecimal.ONE, result.source().balance(), "source balance should be debited"),
                () -> assertEquals(BigDecimal.ONE, result.destination().balance(), "destination balance should be credited"));
    }

    @Test
    void shouldThrowWhenTransferToNullWallet() {
        // Arrange
        Wallet w = createWallet().deposit(BigDecimal.TEN);

        // Act
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> w.transferTo(null, BigDecimal.TEN),
                "transferring to null should throw NPE");

        // Assert
        assertThat(ex.getMessage()).contains("target must not be null");
    }

    @Test
    void shouldThrowWhenTransferAmountIsNull() {
        // Arrange
        Wallet origin = createWallet();
        Wallet destination = createWallet();

        // Act
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> origin.transferTo(destination, null),
                "transferring null amount should throw NPE");

        // Assert
        assertThat(ex.getMessage()).contains("amount must not be null");
    }

    @Test
    void shouldThrowWhenTransferAmountNonPositive() {
        // Arrange
        Wallet origin = createWallet().deposit(BigDecimal.ONE);
        Wallet destination = createWallet();

        // Act
        IllegalArgumentException exZero = assertThrows(
                IllegalArgumentException.class,
                () -> origin.transferTo(destination, BigDecimal.ZERO),
                "transferring zero amount should throw IAE");

        IllegalArgumentException exNegative = assertThrows(IllegalArgumentException.class,
                () -> origin.transferTo(destination, BigDecimal.ONE.negate()),
                "transferring negative amount should throw IAE");

        // Assert
        assertThat(exZero.getMessage()).contains("Transfer amount must be positive");
        assertThat(exNegative.getMessage()).contains("Transfer amount must be positive");
    }

    @Test
    void shouldThrowWhenTransferToSameWallet() {
        // Arrange
        Wallet w = createWallet().deposit(BigDecimal.TEN);

        // Act
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> w.transferTo(w, BigDecimal.TEN),
                "transferring to same wallet should throw IAE");

        // Assert
        assertThat(ex.getMessage()).contains("Cannot transfer to the same wallet");
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────
}
