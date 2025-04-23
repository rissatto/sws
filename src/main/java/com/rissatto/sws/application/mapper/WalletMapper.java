package com.rissatto.sws.application.mapper;

import com.rissatto.sws.domain.Wallet;
import com.rissatto.sws.infrastructure.entity.WalletEntity;

public class WalletMapper {

    private WalletMapper() {
    }

    public static Wallet toDomain(WalletEntity entity) {
        return new Wallet(
                entity.getId(),
                entity.getUserId(),
                entity.getBalance()
        );
    }

    public static WalletEntity toEntity(Wallet domain) {
        WalletEntity entity = new WalletEntity(
                domain.userId(),
                domain.balance()
        );
        entity.setId(domain.id());
        return entity;
    }
}
