package com.rissatto.sws.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rissatto.sws.application.service.WalletService;
import com.rissatto.sws.domain.Wallet;
import com.rissatto.sws.presentation.dto.CreateWalletRequest;
import com.rissatto.sws.presentation.dto.DepositRequest;
import com.rissatto.sws.presentation.dto.TransferRequest;
import com.rissatto.sws.presentation.dto.WithdrawRequest;
import com.rissatto.sws.presentation.exception.RestExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private static Wallet createWallet() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        return new Wallet(id, userId, BigDecimal.ZERO);
    }

    @BeforeEach
    void beforeEach() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(walletController)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    private ResultMatcher[] assertStatusAndHeaderOnWalletCreate(Wallet wallet) {
        return new ResultMatcher[]{
                status().isCreated(),
                header().string("Location", "http://localhost/wallets/" + wallet.id())
        };
    }

    private ResultMatcher[] assertWalletBody(Wallet wallet) {
        return new ResultMatcher[]{
                jsonPath("$.id").value(wallet.id().toString()),
                jsonPath("$.userId").value(wallet.userId().toString()),
                jsonPath("$.balance").value(wallet.balance().toString())
        };
    }

    @Test
    void shouldReturn201AndLocationAndBodyWhenCreatingWallet() throws Exception {
        // Arrange
        Wallet wallet = createWallet();
        when(walletService.create(eq(wallet.userId()), any())).thenReturn(wallet);
        CreateWalletRequest request = new CreateWalletRequest(wallet.userId());

        // Act & Assert
        mockMvc.perform(post("/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(assertStatusAndHeaderOnWalletCreate(wallet))
                .andExpectAll(assertWalletBody(wallet));
    }

    @Test
    void shouldReturn201AndLocationAndBodyWhenCreatingWalletWithIdempotencyKey() throws Exception {
        // Arrange
        Wallet wallet = createWallet();
        String idempotencyKey = UUID.randomUUID().toString();
        when(walletService.create(wallet.userId(), idempotencyKey)).thenReturn(wallet);
        CreateWalletRequest request = new CreateWalletRequest(wallet.userId());

        // Act & Assert
        mockMvc.perform(post("/wallets")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(assertStatusAndHeaderOnWalletCreate(wallet))
                .andExpectAll(assertWalletBody(wallet));
    }

    @Test
    void shouldReturnSameWalletWhenCreatingWithSameIdempotencyKey() throws Exception {
        // Arrange
        Wallet wallet = createWallet();
        String idempotencyKey = UUID.randomUUID().toString();
        when(walletService.create(wallet.userId(), idempotencyKey)).thenReturn(wallet);
        CreateWalletRequest request = new CreateWalletRequest(wallet.userId());

        // Act & Assert
        mockMvc.perform(post("/wallets")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(assertStatusAndHeaderOnWalletCreate(wallet))
                .andExpectAll(assertWalletBody(wallet));

        mockMvc.perform(post("/wallets")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(assertStatusAndHeaderOnWalletCreate(wallet))
                .andExpectAll(assertWalletBody(wallet));
    }

    @Test
    void shouldReturn200AndBodyWhenGettingByIdAExistingWallet() throws Exception {
        // Arrange
        Wallet wallet = createWallet();
        when(walletService.getById(wallet.id())).thenReturn(wallet);

        // Act & Assert
        mockMvc.perform(get("/wallets/{id}", wallet.id()))
                .andExpect(status().isOk())
                .andExpectAll(assertWalletBody(wallet));
    }

    @Test
    void shouldReturn404WhenNotFoundAWallet() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        when(walletService.getById(id)).thenThrow(new EntityNotFoundException("Wallet not found"));

        // Act & Assert
        mockMvc.perform(get("/wallets/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found"));
    }

    @Test
    void shouldGetCurrentBalance() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        when(walletService.getCurrentBalance(id)).thenReturn(BigDecimal.TEN);

        // Act & Assert
        mockMvc.perform(get("/wallets/{id}/balance", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10));
    }

    @Test
    void shouldReturn404WhenGettingCurrentBalanceAndNotFoundAWallet() throws Exception {
        // Arrange
        UUID randomId = UUID.randomUUID();
        when(walletService.getCurrentBalance(randomId)).thenThrow(new EntityNotFoundException("Wallet not found"));

        // Act & Assert
        mockMvc.perform(get("/wallets/{id}/balance", randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found"));
    }

    // region ─ deposit ───────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldReturn200AndBodyWhenDepositingWithoutIdempotencyKey() throws Exception {
        // Arrange
        Wallet wallet = createWallet();
        BigDecimal amount = BigDecimal.ONE;
        Wallet updated = new Wallet(wallet.id(), wallet.userId(), amount);

        when(walletService.deposit(eq(wallet.id()), eq(amount), nullable(String.class)))
                .thenReturn(updated);

        DepositRequest req = new DepositRequest(amount);

        // Act & Assert
        mockMvc.perform(post("/wallets/{id}/deposit", wallet.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpectAll(assertWalletBody(updated));
    }

    @Test
    void shouldReturn200AndBodyWhenDepositingWithIdempotencyKey() throws Exception {
        // Arrange
        Wallet wallet = createWallet();
        BigDecimal amount = BigDecimal.ONE;
        String key = UUID.randomUUID().toString();
        Wallet updated = new Wallet(wallet.id(), wallet.userId(), amount);

        when(walletService.deposit(eq(wallet.id()), eq(amount), eq(key)))
                .thenReturn(updated);

        DepositRequest req = new DepositRequest(amount);

        // Act & Assert
        mockMvc.perform(post("/wallets/{id}/deposit", wallet.id())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpectAll(assertWalletBody(updated));
    }

    @Test
    void shouldReturn404WhenDepositingOnNonExistingWallet() throws Exception {
        // Arrange
        UUID badId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.ONE;

        when(walletService.deposit(eq(badId), eq(amount), nullable(String.class)))
                .thenThrow(new EntityNotFoundException("Wallet not found"));

        DepositRequest req = new DepositRequest(amount);

        // Act & Assert
        mockMvc.perform(post("/wallets/{id}/deposit", badId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found"));
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

    // region ─ withdraw ───────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldReturn200AndBodyWhenWithdrawingWithoutIdempotencyKey() throws Exception {
        // Arrange
        Wallet wallet = createWallet();
        BigDecimal amount = BigDecimal.ONE;
        BigDecimal balance = BigDecimal.ZERO;
        Wallet updated = new Wallet(wallet.id(), wallet.userId(), balance);

        when(walletService.withdraw(eq(wallet.id()), eq(amount), nullable(String.class))).thenReturn(updated);

        WithdrawRequest req = new WithdrawRequest(amount);

        // Act & Assert
        mockMvc.perform(post("/wallets/{id}/withdraw", wallet.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpectAll(assertWalletBody(updated));
    }

    @Test
    void shouldReturn200AndBodyWhenWithdrawingWithIdempotencyKey() throws Exception {
        //Arrange
        Wallet wallet = createWallet();
        BigDecimal amount = BigDecimal.ONE;
        BigDecimal balance = BigDecimal.ZERO;
        String key = UUID.randomUUID().toString();
        Wallet updated = new Wallet(wallet.id(), wallet.userId(), balance);

        when(walletService.withdraw(eq(wallet.id()), eq(amount), eq(key))).thenReturn(updated);

        WithdrawRequest req = new WithdrawRequest(amount);

        // Act & Assert
        mockMvc.perform(post("/wallets/{id}/withdraw", wallet.id())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpectAll(assertWalletBody(updated));
    }

    @Test
    void shouldReturn404WhenWithdrawingOnNonExistingWallet() throws Exception {
        // Arrange
        UUID badId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.ONE;

        when(walletService.withdraw(eq(badId), eq(amount), nullable(String.class)))
                .thenThrow(new EntityNotFoundException("Wallet not found"));

        WithdrawRequest req = new WithdrawRequest(amount);

        // Act & Assert
        mockMvc.perform(post("/wallets/{id}/withdraw", badId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found"));
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

    // region ─ transfer ───────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldReturn200AndBodyWhenTransferringWithoutIdempotencyKey() throws Exception {
        // Arrange
        Wallet wallet = createWallet();
        BigDecimal amount = BigDecimal.ONE;
        UUID target = UUID.randomUUID();
        Wallet updated = new Wallet(wallet.id(), wallet.userId(), BigDecimal.ZERO);

        when(walletService.transfer(eq(wallet.id()), eq(target), eq(amount), nullable(String.class)))
                .thenReturn(updated);

        TransferRequest req = new TransferRequest(target, amount);

        // Act & Assert
        mockMvc.perform(post("/wallets/{id}/transfer", wallet.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpectAll(assertWalletBody(updated));
    }

    @Test
    void shouldReturn200AndBodyWhenTransferringWithIdempotencyKey() throws Exception {
        // Arrange
        Wallet wallet = createWallet();
        BigDecimal amount = BigDecimal.ONE;
        UUID target = UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        Wallet updated = new Wallet(wallet.id(), wallet.userId(), BigDecimal.ZERO);

        when(walletService.transfer(eq(wallet.id()), eq(target), eq(amount), eq(key)))
                .thenReturn(updated);

        TransferRequest req = new TransferRequest(target, amount);

        // Act & Assert
        mockMvc.perform(post("/wallets/{id}/transfer", wallet.id())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpectAll(assertWalletBody(updated));
    }

    @Test
    void shouldReturn404WhenTransferringOnNonExistingWallet() throws Exception {
        // Arrange
        UUID badId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.ONE;
        UUID target = UUID.randomUUID();

        when(walletService.transfer(eq(badId), eq(target), eq(amount), nullable(String.class)))
                .thenThrow(new EntityNotFoundException("Wallet not found"));

        TransferRequest req = new TransferRequest(target, amount);

        // Act & Assert
        mockMvc.perform(post("/wallets/{id}/transfer", badId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found"));
    }

    // endregion ────────────────────────────────────────────────────────────────────────────────────────────────────────
}
