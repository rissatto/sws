package com.rissatto.sws.presentation.controller;

import com.rissatto.sws.presentation.dto.CreateUserRequest;
import com.rissatto.sws.presentation.dto.UserResponse;
import com.rissatto.sws.presentation.exception.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/users";
    }

    @Test
    void shouldCreateUserAndReturn201() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest("John Doe");

        // Act
        ResponseEntity<UserResponse> response = restTemplate.postForEntity(baseUrl(), request, UserResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UserResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.name()).isEqualTo("John Doe");
        assertThat(Objects.requireNonNull(response.getHeaders().getLocation()).toString()).startsWith(baseUrl() + "/");
    }

    @Test
    void shouldReturnSameUserWhenCreatingWithSameIdempotencyKey() {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();
        CreateUserRequest request = new CreateUserRequest("Richard Roe");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<UserResponse> response1 = restTemplate.exchange(
                baseUrl(), HttpMethod.POST, entity, UserResponse.class);
        ResponseEntity<UserResponse> response2 = restTemplate.exchange(
                baseUrl(), HttpMethod.POST, entity, UserResponse.class);

        // Assert
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UserResponse u1 = response1.getBody();
        UserResponse u2 = response2.getBody();
        assertThat(u1).isEqualTo(u2);
    }

    @Test
    void shouldGetUserAndReturn200() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest("John Doe");
        ResponseEntity<UserResponse> createResponse = restTemplate.postForEntity(
                baseUrl(), request, UserResponse.class);
        String location = Objects.requireNonNull(createResponse.getHeaders().getLocation()).toString();

        // Act
        ResponseEntity<UserResponse> getResponse = restTemplate.getForEntity(location, UserResponse.class);

        // Assert
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isEqualTo(createResponse.getBody());
    }

    @Test
    void shouldReturn404WhenGettingNonExistingUser() {
        // Arrange
        UUID randomId = UUID.randomUUID();

        // Act
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                baseUrl() + "/" + randomId,
                ErrorResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertNotNull(response.getBody());
        assertThat(response.getBody().message()).isEqualTo("user not found");
    }
}