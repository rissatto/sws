package com.rissatto.sws.domain;

import java.util.Objects;
import java.util.UUID;

public record User(UUID id, String name) {

    public User {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        name = name.trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    public static User create(String name) {
        return new User(UUID.randomUUID(), name);
    }
}
