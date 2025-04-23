package com.rissatto.sws.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
@JsonPropertyOrder({"id", "name", "createdAt", "updatedAt"})
public class UserEntity extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Default constructor for JPA
     */
    @SuppressWarnings("unused")
    protected UserEntity() {
    }

    public UserEntity(String name) {
        this.name = Objects.requireNonNull(name).trim();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name).trim();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UserEntity that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                "} " + super.toString();
    }
}
