package com.rissatto.sws.application.mapper;

import com.rissatto.sws.domain.User;
import com.rissatto.sws.infrastructure.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    @Test
    void toEntity_shouldMapDomainToEntity() {
        UUID id = UUID.randomUUID();
        User domain = new User(id, "John Doe");

        UserEntity e = UserMapper.toEntity(domain);

        assertThat(e.getId()).isEqualTo(id);
        assertThat(e.getName()).isEqualTo("John Doe");
    }

    @Test
    void toDomain_shouldMapEntityToDomain() {
        UUID id = UUID.randomUUID();
        UserEntity e = new UserEntity("John Doe");
        e.setId(id);

        User domain = UserMapper.toDomain(e);

        assertThat(domain.id()).isEqualTo(id);
        assertThat(domain.name()).isEqualTo("John Doe");
    }
}
