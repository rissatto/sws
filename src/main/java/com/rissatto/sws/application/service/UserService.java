package com.rissatto.sws.application.service;

import com.rissatto.sws.domain.User;

import java.util.UUID;

public interface UserService {

    User create(String name);

    User create(String name, String idempotencyKey);

    User getById(UUID id);

}
