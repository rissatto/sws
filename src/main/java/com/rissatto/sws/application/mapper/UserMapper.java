package com.rissatto.sws.application.mapper;

import com.rissatto.sws.domain.User;
import com.rissatto.sws.infrastructure.entity.UserEntity;

public class UserMapper {

    private UserMapper() {

    }

    public static UserEntity toEntity(User domain) {
        UserEntity entity = new UserEntity(domain.name());
        entity.setId(domain.id());
        return entity;
    }

    public static User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getName());
    }
}