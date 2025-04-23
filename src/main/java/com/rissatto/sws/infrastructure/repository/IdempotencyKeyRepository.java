package com.rissatto.sws.infrastructure.repository;

import com.rissatto.sws.infrastructure.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
    Optional<IdempotencyKey> findByIdempotencyKeyAndOperation(String idempotencyKey, String operation);
}
