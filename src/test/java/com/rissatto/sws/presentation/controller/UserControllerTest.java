package com.rissatto.sws.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rissatto.sws.application.service.UserService;
import com.rissatto.sws.domain.User;
import com.rissatto.sws.presentation.dto.CreateUserRequest;
import com.rissatto.sws.presentation.exception.RestExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private static User createUser(String name) {
        UUID id = UUID.randomUUID();
        return new User(id, name);
    }

    @BeforeEach
    void beforeEach() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    private ResultMatcher[] assertStatusAndHeaderOnUserCreate(User user) {
        return new ResultMatcher[]{
                status().isCreated(),
                header().string("Location", "http://localhost/users/" + user.id())
        };
    }

    private ResultMatcher[] assertUserBody(User user) {
        return new ResultMatcher[]{
                jsonPath("$.id").value(user.id().toString()),
                jsonPath("$.name").value(user.name())
        };
    }

    @Test
    void shouldReturn201AndLocationAndBodyWhenCreatingUser() throws Exception {
        // Arrange
        User user = createUser("John Doe");
        when(userService.create(eq(user.name()), nullable(String.class))).thenReturn(user);
        CreateUserRequest request = new CreateUserRequest(user.name());

        // Act & Assert
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(assertStatusAndHeaderOnUserCreate(user))
                .andExpectAll(assertUserBody(user));
    }

    @Test
    void shouldReturn201AndLocationAndBodyWhenCreatingUserWithIdempotencyKey() throws Exception {
        // Arrange
        User user = createUser("Jane Doe");
        String idempotencyKey = UUID.randomUUID().toString();
        when(userService.create(user.name(), idempotencyKey)).thenReturn(user);
        CreateUserRequest request = new CreateUserRequest(user.name());

        // Act & Assert
        mockMvc.perform(post("/users")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(assertStatusAndHeaderOnUserCreate(user))
                .andExpectAll(assertUserBody(user));
    }

    @Test
    void shouldReturnSameUserWhenCreatingWithSameIdempotencyKey() throws Exception {
        // Arrange
        User user = createUser("Jane Doe");
        String idempotencyKey = UUID.randomUUID().toString();
        when(userService.create(user.name(), idempotencyKey)).thenReturn(user);
        CreateUserRequest request = new CreateUserRequest(user.name());

        // Act & Assert
        // First request
        mockMvc.perform(post("/users")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(assertStatusAndHeaderOnUserCreate(user))
                .andExpectAll(assertUserBody(user));

        // Second request with same key
        mockMvc.perform(post("/users")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(assertStatusAndHeaderOnUserCreate(user))
                .andExpectAll(assertUserBody(user));
    }

    @Test
    void shouldReturn200AndUserWhenGettingByIdAExistingUser() throws Exception {
        // Arrange
        User user = createUser("Richard Roe");
        when(userService.getById(user.id())).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/users/{id}", user.id()))
                .andExpect(status().isOk())
                .andExpectAll(assertUserBody(user));
    }

    @Test
    void shouldReturn404WhenNotFoundAnUser() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        when(userService.getById(id)).thenThrow(new EntityNotFoundException("user not found"));

        // Act & Assert
        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("user not found"));
    }
}