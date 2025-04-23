package com.rissatto.sws.infrastructure.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class WalletEntity extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    /**
     * Default constructor for JPA
     */
    @SuppressWarnings("unused")
    protected WalletEntity() {
    }

    public WalletEntity(UUID userId, BigDecimal balance) {
        this.userId = userId;
        this.balance = balance;
    }

    @SuppressWarnings("unused")
    @PrePersist
    protected void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WalletEntity that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "WalletEntity{" +
                "id=" + id +
                ", userId=" + userId +
                ", balance=" + balance +
                "} " + super.toString();
    }
}
