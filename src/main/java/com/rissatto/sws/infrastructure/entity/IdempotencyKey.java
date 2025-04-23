package com.rissatto.sws.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey extends Auditable {

    @Id
    @Column(nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private String operation;

    @Column(nullable = false)
    private UUID resourceId;

    /**
     * Default constructor for JPA
     */
    @SuppressWarnings("unused")
    public IdempotencyKey() {
    }

    public IdempotencyKey(String idempotencyKey, String operation, UUID resourceId) {
        this.idempotencyKey = idempotencyKey;
        this.operation = operation;
        this.resourceId = resourceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getOperation() {
        return operation;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    @Override
    public String toString() {
        return "IdempotencyKey{" +
                "key='" + idempotencyKey + '\'' +
                ", operation='" + operation + '\'' +
                ", resourceId=" + resourceId +
                "} " + super.toString();
    }
}