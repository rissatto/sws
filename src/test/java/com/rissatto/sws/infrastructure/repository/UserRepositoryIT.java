package com.rissatto.sws.infrastructure.repository;

import com.rissatto.sws.infrastructure.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UserRepositoryIT extends AbstractRepositoryIT {

    private static final Logger log = LoggerFactory.getLogger(UserRepositoryIT.class);

    private final UserRepository userRepository;

    @Autowired
    public UserRepositoryIT(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Test
    void shouldSaveAndFindByIdWithAuditData() {
        // Arrange
        UserEntity user = new UserEntity("John Doe");

        // Act
        userRepository.save(user);
        UUID id = user.getId();
        Optional<UserEntity> userFound = userRepository.findById(id);

        // Assert
        assertThat(userFound).isPresent()
                .get()
                .satisfies(db -> {
                    log.info("\n🎯 User found in database by id:\n{}", db);
                    assertThat(db.getName()).isEqualTo("John Doe");
                    assertThat(db.getCreatedAt()).isNotNull();
                    assertThat(db.getUpdatedAt()).isNotNull();
                });
    }

    @Test
    void shouldFindByName() {
        // Arrange
        UserEntity u = new UserEntity("Baby Doe");
        userRepository.save(u);

        // Act
        Optional<UserEntity> found = userRepository.findByName("Baby Doe");

        // Assert
        assertThat(found).isPresent()
                .get()
                .satisfies(db -> {
                    log.info("\n🎯 User found in database by name:\n{}", db);
                    assertThat(db.getId()).isEqualTo(u.getId());
                    assertThat(db.getName()).isEqualTo("Baby Doe");
                });
    }

}
