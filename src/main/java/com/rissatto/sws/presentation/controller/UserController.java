package com.rissatto.sws.presentation.controller;

import com.rissatto.sws.application.service.UserService;
import com.rissatto.sws.domain.User;
import com.rissatto.sws.presentation.dto.CreateUserRequest;
import com.rissatto.sws.presentation.dto.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestBody CreateUserRequest request) {
        User user = userService.create(request.name(), idempotencyKey);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(user.id())
                .toUri();
        return ResponseEntity
                .created(location)
                .body(new UserResponse(user.id(), user.name()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        User user = userService.getById(id);
        return ResponseEntity.ok(new UserResponse(user.id(), user.name()));
    }
}