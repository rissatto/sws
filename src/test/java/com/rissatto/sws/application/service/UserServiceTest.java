package com.rissatto.sws.application.service;

import com.rissatto.sws.domain.User;
import com.rissatto.sws.infrastructure.entity.IdempotencyKey;
import com.rissatto.sws.infrastructure.entity.UserEntity;
import com.rissatto.sws.infrastructure.repository.IdempotencyKeyRepository;
import com.rissatto.sws.infrastructure.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    private static final String CREATE_OPERATION = "createUser";
    @Mock
    private UserRepository repository;
    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;
    @InjectMocks
    private UserServiceImpl service;

    @Test
    void shouldCreateUserWithTrimmedName() {
        // Arrange
        String rawName = "  John Doe  ";
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);

        // Act
        User u = service.create(rawName);

        // Assert
        verify(repository).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("John Doe");
        assertThat(saved.getId()).isEqualTo(u.id());
    }

    @Test
    void shouldCreateUserWithIdempotencyKey() {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();
        ArgumentCaptor<IdempotencyKey> keyCaptor = ArgumentCaptor.forClass(IdempotencyKey.class);

        // Act
        User u = service.create("John Doe", idempotencyKey);

        // Assert
        verify(repository).save(any(UserEntity.class));
        verify(idempotencyKeyRepository).save(keyCaptor.capture());
        IdempotencyKey saved = keyCaptor.getValue();
        assertThat(saved.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(saved.getOperation()).isEqualTo(CREATE_OPERATION);
        assertThat(saved.getResourceId()).isEqualTo(u.id());
    }

    @Test
    void shouldReturnSameUserWhenUsingSameIdempotencyKeyToCreate() {
        // Arrange
        UUID existingId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        when(idempotencyKeyRepository.findByIdempotencyKeyAndOperation(idempotencyKey, CREATE_OPERATION))
                .thenReturn(Optional.of(new IdempotencyKey(idempotencyKey, CREATE_OPERATION, existingId)));
        UserEntity e = new UserEntity("Jane Doe");
        e.setId(existingId);
        when(repository.findById(existingId)).thenReturn(Optional.of(e));

        // Act
        User u = service.create("Jane Doe", idempotencyKey);

        // Assert
        verify(repository, never()).save(any());
        verify(idempotencyKeyRepository, never()).save(any());
        assertThat(u.id()).isEqualTo(existingId);
        assertThat(u.name()).isEqualTo("Jane Doe");
    }

    @Test
    void shouldThrowWhenCreatingWithNullName() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("name must not be null");
    }

    @Test
    void shouldThrowWhenCreatingWithBlankName() {
        assertThatThrownBy(() -> service.create("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
    }

    @Test
    void shouldThrowWhenNotFound() {
        // Arrange
        UUID existingId = UUID.randomUUID();
        when(repository.findById(existingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getById(existingId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("user not found");
    }

    @Test
    void shouldFindUserById() {
        // Arrange
        UUID existingId = UUID.randomUUID();
        UserEntity e = new UserEntity("John Doe");
        e.setId(existingId);
        when(repository.findById(existingId)).thenReturn(Optional.of(e));

        // Act
        User u = service.getById(existingId);

        // Assert
        assertThat(u.id()).isEqualTo(existingId);
        assertThat(u.name()).isEqualTo("John Doe");
    }

}
