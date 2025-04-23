package com.rissatto.sws.application.mapper;

import com.rissatto.sws.domain.Wallet;
import com.rissatto.sws.infrastructure.entity.WalletEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalletMapperTest {

    @Test
    void toEntity_shouldMapDomainToEntity() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Wallet domain = new Wallet(id, userId, BigDecimal.ZERO);

        WalletEntity entity = WalletMapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getUserId()).isEqualTo(userId);
        assertThat(entity.getBalance()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void toDomain_shouldMapEntityToDomain() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WalletEntity entity = new WalletEntity(userId, BigDecimal.ZERO);
        entity.setId(id);

        Wallet domain = WalletMapper.toDomain(entity);

        assertThat(domain.id()).isEqualTo(id);
        assertThat(domain.userId()).isEqualTo(userId);
        assertThat(domain.balance()).isEqualTo(BigDecimal.ZERO);
    }
}
