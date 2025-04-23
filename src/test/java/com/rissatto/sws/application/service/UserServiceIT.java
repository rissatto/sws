package com.rissatto.sws.application.service;

import com.rissatto.sws.domain.User;
import com.rissatto.sws.infrastructure.entity.UserEntity;
import com.rissatto.sws.infrastructure.repository.IdempotencyKeyRepository;
import com.rissatto.sws.infrastructure.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIT {

    private static final String CREATE_OPERATION = "createUser";
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Test
    void shouldCreateUser() {
        // Act
        User user = userService.create("John Doe");

        // Assert
        assertThat(user).isNotNull();
        assertThat(user.id()).isNotNull();
        assertThat(user.name()).isEqualTo("John Doe");
        UserEntity entity = userRepository.findById(user.id())
                .orElseThrow(() -> new AssertionError("UserEntity not found in repository"));
        assertThat(entity.getName()).isEqualTo("John Doe");
    }

    @Test
    void shouldNotCreateDuplicateUserWhenUsingSameIdempotencyKey() {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();

        // Act
        User first = userService.create("Jane Doe", idempotencyKey);
        User second = userService.create("Jane Doe", idempotencyKey);

        UserEntity found = userRepository.findById(first.id()).orElseThrow();

        // Assert
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(found.getId()).isEqualTo(first.id());

        assertThat(idempotencyKeyRepository
                .findByIdempotencyKeyAndOperation(idempotencyKey, CREATE_OPERATION))
                .isPresent();
    }

    @Test
    void shouldGetByIdExistingUser() {
        // Arrange
        User created = userService.create("Richard Roe");
        UUID userId = created.id();

        // Act
        User fetched = userService.getById(userId);

        // Assert
        assertThat(fetched.id()).isEqualTo(userId);
        assertThat(fetched.name()).isEqualTo("Richard Roe");
    }

    @Test
    void shouldThrowWhenGettingANotExistUser() {
        // Act & Assert
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> userService.getById(randomId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("user not found");
    }
}
