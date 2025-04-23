package com.rissatto.sws.presentation.controller;

import com.rissatto.sws.application.service.WalletService;
import com.rissatto.sws.domain.Wallet;
import com.rissatto.sws.presentation.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final WalletService walletService;

    @Autowired
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<WalletResponse> create(@RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false)
                                                 String idempotencyKey,
                                                 @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.create(request.userId(), idempotencyKey);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(wallet.id())
                .toUri();
        WalletResponse response = new WalletResponse(wallet.id(), wallet.userId(), wallet.balance());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponse> getById(@PathVariable UUID id) {
        Wallet wallet = walletService.getById(id);
        WalletResponse response = new WalletResponse(wallet.id(), wallet.userId(), wallet.balance());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<GetBalanceResponse> getBalance(@PathVariable UUID id,
                                                         @RequestParam(name = "at", required = false)
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                         LocalDateTime at) {
        BigDecimal balance = (at == null)
                ? walletService.getCurrentBalance(id)
                : walletService.getHistoricalBalance(id, at.atZone(ZoneOffset.UTC).toInstant());
        return ResponseEntity.ok(new GetBalanceResponse(balance));
    }

    @PostMapping("/{walletId}/deposit")
    public ResponseEntity<WalletResponse> deposit(@PathVariable UUID walletId,
                                                  @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false)
                                                  String idempotencyKey,
                                                  @RequestBody DepositRequest request) {
        Wallet wallet = walletService.deposit(walletId, request.amount(), idempotencyKey);
        return ResponseEntity.ok(new WalletResponse(wallet.id(), wallet.userId(), wallet.balance()));
    }

    @PostMapping("/{walletId}/withdraw")
    public ResponseEntity<WalletResponse> withdraw(@PathVariable UUID walletId,
                                                   @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false)
                                                   String idempotencyKey,
                                                   @RequestBody WithdrawRequest request) {
        Wallet wallet = walletService.withdraw(walletId, request.amount(), idempotencyKey);
        return ResponseEntity.ok(new WalletResponse(wallet.id(), wallet.userId(), wallet.balance()));
    }

    @PostMapping("/{walletId}/transfer")
    public ResponseEntity<WalletResponse> transfer(@PathVariable UUID walletId,
                                                   @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false)
                                                   String idempotencyKey,
                                                   @RequestBody TransferRequest request) {
        Wallet wallet = walletService.transfer(walletId, request.targetWalletId(), request.amount(), idempotencyKey);
        return ResponseEntity.ok(new WalletResponse(wallet.id(), wallet.userId(), wallet.balance()));
    }

}