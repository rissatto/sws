package com.rissatto.sws.application.service;

import com.rissatto.sws.application.mapper.TransactionMapper;
import com.rissatto.sws.application.mapper.WalletMapper;
import com.rissatto.sws.domain.Transaction;
import com.rissatto.sws.domain.Wallet;
import com.rissatto.sws.infrastructure.entity.IdempotencyKey;
import com.rissatto.sws.infrastructure.entity.TransactionEntity;
import com.rissatto.sws.infrastructure.entity.WalletEntity;
import com.rissatto.sws.infrastructure.repository.IdempotencyKeyRepository;
import com.rissatto.sws.infrastructure.repository.TransactionRepository;
import com.rissatto.sws.infrastructure.repository.WalletRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private static final String CREATE_OPERATION = "createWallet";
    private static final String DEPOSIT_OPERATION = "depositWallet";
    private static final String WITHDRAW_OPERATION = "withdrawWallet";
    private static final String TRANSFER_OPERATION = "transferWallet";
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    public WalletServiceImpl(WalletRepository walletRepository, TransactionRepository transactionRepository, IdempotencyKeyRepository idempotencyKeyRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Override
    public Wallet create(UUID userId) {
        return create(userId, null);
    }

    @Override
    @Transactional
    public Wallet create(UUID userId, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByIdempotencyKeyAndOperation(idempotencyKey, CREATE_OPERATION);
            if (existingKey.isPresent()) {
                UUID resourceId = existingKey.get().getResourceId();
                return getById(resourceId);
            }
        }

        Wallet domain = Wallet.create(userId);
        WalletEntity toSave = WalletMapper.toEntity(domain);
        WalletEntity saved = walletRepository.save(toSave);

        if (idempotencyKey != null) {
            idempotencyKeyRepository.save(new IdempotencyKey(idempotencyKey, CREATE_OPERATION, saved.getId()));
        }

        return WalletMapper.toDomain(saved);
    }

    @Override
    public Wallet getById(UUID walletId) {
        WalletEntity entity = walletRepository.findById(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));
        return WalletMapper.toDomain(entity);
    }

    @Override
    public BigDecimal getCurrentBalance(UUID walletId) {
        return getById(walletId).balance();
    }

    @Override
    @Transactional
    public Wallet deposit(UUID walletId, BigDecimal amount) {
        return deposit(walletId, amount, null);
    }

    @Override
    @Transactional
    public Wallet deposit(UUID walletId, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<IdempotencyKey> existing = idempotencyKeyRepository
                    .findByIdempotencyKeyAndOperation(idempotencyKey, DEPOSIT_OPERATION);
            if (existing.isPresent()) {
                UUID resourceId = existing.get().getResourceId();
                return getById(resourceId);
            }
        }

        WalletEntity walletEntity = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));

        Wallet walletDomain = WalletMapper.toDomain(walletEntity);

        Wallet updatedWallet = walletDomain.deposit(amount);

        WalletEntity updatedWalletEntity = WalletMapper.toEntity(updatedWallet);
        walletRepository.save(updatedWalletEntity);

        Transaction transaction = Transaction.createDeposit(walletId, amount);
        TransactionEntity transactionEntity = TransactionMapper.toEntity(transaction);
        transactionRepository.save(transactionEntity);

        if (idempotencyKey != null) {
            idempotencyKeyRepository.save(new IdempotencyKey(idempotencyKey, DEPOSIT_OPERATION, updatedWallet.id()));
        }

        return updatedWallet;
    }

    @Override
    @Transactional
    public Wallet withdraw(UUID walletId, BigDecimal amount) {
        return withdraw(walletId, amount, null);
    }

    @Override
    @Transactional
    public Wallet withdraw(UUID walletId, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<IdempotencyKey> existing = idempotencyKeyRepository
                    .findByIdempotencyKeyAndOperation(idempotencyKey, WITHDRAW_OPERATION);
            if (existing.isPresent()) {
                UUID resourceId = existing.get().getResourceId();
                return getById(resourceId);
            }
        }

        WalletEntity walletEntity = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));

        Wallet walletDomain = WalletMapper.toDomain(walletEntity);

        Wallet updatedWallet = walletDomain.withdraw(amount);

        WalletEntity updatedWalletEntity = WalletMapper.toEntity(updatedWallet);
        walletRepository.save(updatedWalletEntity);

        Transaction transaction = Transaction.createWithdrawal(walletId, amount);
        TransactionEntity transactionEntity = TransactionMapper.toEntity(transaction);
        transactionRepository.save(transactionEntity);

        if (idempotencyKey != null) {
            idempotencyKeyRepository.save(new IdempotencyKey(idempotencyKey, WITHDRAW_OPERATION, updatedWallet.id()));
        }

        return updatedWallet;
    }

    @Override
    @Transactional
    public Wallet transfer(UUID sourceWalletId, UUID targetWalletId, BigDecimal amount) {
        return transfer(sourceWalletId, targetWalletId, amount, null);
    }

    @Override
    @Transactional
    public Wallet transfer(UUID sourceWalletId, UUID targetWalletId, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByIdempotencyKeyAndOperation(idempotencyKey, TRANSFER_OPERATION);
            if (existing.isPresent()) {
                UUID resourceId = existing.get().getResourceId();
                return getById(resourceId);
            }
        }

        WalletEntity sourceEntity = walletRepository.findByIdForUpdate(sourceWalletId)
                .orElseThrow(() -> new EntityNotFoundException("Source Wallet not found"));
        WalletEntity targetEntity = walletRepository.findByIdForUpdate(targetWalletId)
                .orElseThrow(() -> new EntityNotFoundException("Target Wallet not found"));

        Wallet sourceWallet = WalletMapper.toDomain(sourceEntity);
        Wallet targetWallet = WalletMapper.toDomain(targetEntity);

        Wallet.TransferResult transferResult = sourceWallet.transferTo(targetWallet, amount);

        Wallet updatedSource = transferResult.source();
        Wallet updatedDestination = transferResult.destination();

        WalletEntity updatedSourceEntity = WalletMapper.toEntity(updatedSource);
        WalletEntity updatedDestinationEntity = WalletMapper.toEntity(updatedDestination);

        walletRepository.save(updatedSourceEntity);
        walletRepository.save(updatedDestinationEntity);

        Transaction outTransaction = Transaction.createTransferOut(sourceWalletId, amount);
        Transaction inTransaction = Transaction.createTransferIn(targetWalletId, amount);

        transactionRepository.save(TransactionMapper.toEntity(outTransaction));
        transactionRepository.save(TransactionMapper.toEntity(inTransaction));

        if (idempotencyKey != null) {
            idempotencyKeyRepository.save(new IdempotencyKey(idempotencyKey, TRANSFER_OPERATION, updatedSource.id()));
        }

        return updatedSource;
    }

    @Override
    public BigDecimal getHistoricalBalance(UUID walletId, Instant at) {
        walletRepository.findById(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));

        return transactionRepository
                .findByWalletIdAndTimestampLessThanEqual(walletId, at)
                .stream()
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
