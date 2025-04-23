package com.rissatto.sws.application.service;

import com.rissatto.sws.application.mapper.UserMapper;
import com.rissatto.sws.domain.User;
import com.rissatto.sws.infrastructure.entity.IdempotencyKey;
import com.rissatto.sws.infrastructure.entity.UserEntity;
import com.rissatto.sws.infrastructure.repository.IdempotencyKeyRepository;
import com.rissatto.sws.infrastructure.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final String OPERATION = "createUser";
    private final UserRepository repository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, IdempotencyKeyRepository idempotencyKeyRepository) {
        this.repository = userRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Override
    public User create(String name) {
        return create(name, null);
    }

    @Override
    @Transactional
    public User create(String name, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<IdempotencyKey> existingIdempotencyKey = idempotencyKeyRepository.findByIdempotencyKeyAndOperation(idempotencyKey, OPERATION);
            if (existingIdempotencyKey.isPresent()) {
                UUID resourceId = existingIdempotencyKey.get().getResourceId();
                return getById(resourceId);
            }
        }

        User domain = User.create(name);
        UserEntity entity = UserMapper.toEntity(domain);
        repository.save(entity);

        if (idempotencyKey != null) {
            idempotencyKeyRepository.save(new IdempotencyKey(idempotencyKey, OPERATION, entity.getId()));
        }

        return UserMapper.toDomain(entity);
    }

    @Override
    public User getById(UUID id) {
        UserEntity entity = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("user not found"));
        return UserMapper.toDomain(entity);
    }
}
