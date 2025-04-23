package com.rissatto.sws.infrastructure.entity;

import com.rissatto.sws.domain.Transaction;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity extends Auditable {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Transaction.Type type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant timestamp;

    /**
     * Default constructor for JPA
     */
    @SuppressWarnings("unused")
    protected TransactionEntity() {
    }

    public TransactionEntity(UUID id, UUID walletId, Transaction.Type type, BigDecimal amount, Instant timestamp) {
        this.id = id;
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public TransactionEntity(UUID walletId, Transaction.Type type, BigDecimal amount, Instant timestamp) {
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    @SuppressWarnings("unused")
    @PrePersist
    protected void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    // Getters e Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public Transaction.Type getType() {
        return type;
    }

    public void setType(Transaction.Type type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof TransactionEntity that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "TransactionEntity{" +
                "id=" + id +
                ", walletId=" + walletId +
                ", type=" + type +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                "} " + super.toString();
    }
}