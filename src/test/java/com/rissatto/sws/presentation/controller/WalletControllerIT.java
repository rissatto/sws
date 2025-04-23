package com.rissatto.sws.presentation.controller;

import com.rissatto.sws.application.service.UserService;
import com.rissatto.sws.presentation.dto.*;
import com.rissatto.sws.presentation.exception.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WalletControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserService userService;
    private UUID globalUserId;

    private String baseUrl() {
        return "http://localhost:" + port + "/wallets";
    }

    @BeforeEach
    void beforeEach() {
        if (globalUserId == null) {
            globalUserId = userService.create("John Doe").id();
        }
    }

    @Test
    void shouldCreateWalletAndReturn201() {
        // Arrange
        CreateWalletRequest request = new CreateWalletRequest(globalUserId);

        // Act
        ResponseEntity<WalletResponse> response = restTemplate.postForEntity(baseUrl(), request, WalletResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        WalletResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.userId()).isEqualTo(globalUserId);
        assertThat(Objects.requireNonNull(response.getHeaders().getLocation()).toString()).startsWith(baseUrl() + "/");
    }

    @Test
    void shouldCreateWalletAndReturn201UsingIdempotencyKey() {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();
        CreateWalletRequest request = new CreateWalletRequest(globalUserId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<WalletResponse> response = restTemplate.exchange(
                baseUrl(), HttpMethod.POST, entity, WalletResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        WalletResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.userId()).isEqualTo(globalUserId);
        assertThat(Objects.requireNonNull(response.getHeaders().getLocation()).toString()).startsWith(baseUrl() + "/");
    }

    @Test
    void shouldReturnSameWalletWhenCreatingWithSameIdempotencyKey() {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();
        CreateWalletRequest request = new CreateWalletRequest(globalUserId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<WalletResponse> response1 = restTemplate.exchange(
                baseUrl(), HttpMethod.POST, entity, WalletResponse.class);
        ResponseEntity<WalletResponse> response2 = restTemplate.exchange(
                baseUrl(), HttpMethod.POST, entity, WalletResponse.class);

        // Assert
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertNotNull(response1.getBody());
        assertNotNull(response2.getBody());
        assertThat(response1.getBody().id()).isEqualTo(response2.getBody().id());
        assertThat(response1.getBody().userId()).isEqualTo(response2.getBody().userId());
        assertThat(response1.getBody().balance()).isEqualByComparingTo(response2.getBody().balance());
    }

    @Test
    void shouldGetWalletAndReturn200() {
        // Arrange
        CreateWalletRequest request = new CreateWalletRequest(globalUserId);
        ResponseEntity<WalletResponse> createResponse = restTemplate.postForEntity(
                baseUrl(), request, WalletResponse.class);
        String location = Objects.requireNonNull(createResponse.getHeaders().getLocation()).toString();

        // Act
        ResponseEntity<WalletResponse> getResponse = restTemplate.getForEntity(location, WalletResponse.class);

        // Assert
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertNotNull(getResponse.getBody());
        assertNotNull(createResponse.getBody());
        assertThat(getResponse.getBody().id()).isEqualTo(createResponse.getBody().id());
        assertThat(getResponse.getBody().userId()).isEqualTo(createResponse.getBody().userId());
        assertThat(getResponse.getBody().balance()).isEqualByComparingTo(createResponse.getBody().balance());
    }

    @Test
    void shouldReturn404WhenGettingNonExistingWallet() {
        // Arrange
        UUID randomId = UUID.randomUUID();

        // Act
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                baseUrl() + "/" + randomId, ErrorResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertNotNull(response.getBody());
        assertThat(response.getBody().message()).isEqualTo("Wallet not found");
    }

    // region ─ balance ────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldGetCurrentBalance() {
        // Arrange
        CreateWalletRequest request = new CreateWalletRequest(globalUserId);
        ResponseEntity<WalletResponse> createResponse = restTemplate.postForEntity(
                baseUrl(), request, WalletResponse.class);
        String walletId = Objects.requireNonNull(createResponse.getBody()).id().toString();

        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl() + "/" + walletId + "/balance", String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertNotNull(response.getBody());
        assertThat(response.getBody()).contains("\"balance\":0");
    }

    @Test
    void shouldGetHistoricalBalance() {
        // Arrange
        String base = Objects.requireNonNull(
                restTemplate.postForEntity(baseUrl(), new CreateWalletRequest(globalUserId), WalletResponse.class)
                        .getHeaders().getLocation()
        ).toString();
        restTemplate.postForEntity(base + "/deposit", new DepositRequest(BigDecimal.TWO), WalletResponse.class);
        restTemplate.postForEntity(base + "/withdraw", new WithdrawRequest(BigDecimal.ONE), WalletResponse.class);
        Instant moment = Instant.now();
        String iso = moment.toString();
        restTemplate.postForEntity(base + "/deposit", new DepositRequest(BigDecimal.TEN), WalletResponse.class);

        // Act
        String url = base + "/balance?at={at}";
        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class, iso);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"balance\":1");
    }

    @Test
    void shouldReturn404WhenGettingBalanceAndNotFoundAWallet() {
        // Arrange
        UUID randomId = UUID.randomUUID();

        // Act
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                baseUrl() + "/" + randomId + "/balance", ErrorResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertNotNull(response.getBody());
        assertThat(response.getBody().message()).isEqualTo("Wallet not found");
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

    // region ─ deposit ────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldDepositAndReturn200() {
        // Arrange
        CreateWalletRequest createRequest = new CreateWalletRequest(globalUserId);
        String walletUrl = Objects.requireNonNull(
                restTemplate.postForEntity(baseUrl(), createRequest, WalletResponse.class)
                        .getHeaders().getLocation()
        ).toString();

        BigDecimal amount = BigDecimal.TEN;
        DepositRequest request = new DepositRequest(amount);

        // Act
        ResponseEntity<WalletResponse> response = restTemplate.postForEntity(
                walletUrl + "/deposit", request, WalletResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertNotNull(response.getBody());
        assertThat(response.getBody().balance()).isEqualByComparingTo(amount);
    }

    @Test
    void shouldDepositAndReturn200UsingIdempotencyKey() {
        // Arrange
        CreateWalletRequest createRequest = new CreateWalletRequest(globalUserId);
        String walletUrl = Objects.requireNonNull(
                restTemplate.postForEntity(baseUrl(), createRequest, WalletResponse.class)
                        .getHeaders().getLocation()
        ).toString();

        BigDecimal amount = BigDecimal.TEN;
        String key = UUID.randomUUID().toString();
        DepositRequest request = new DepositRequest(amount);
        HttpHeaders h = new HttpHeaders();
        h.add("Idempotency-Key", key);
        h.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<WalletResponse> response = restTemplate.exchange(
                walletUrl + "/deposit",
                HttpMethod.POST,
                new HttpEntity<>(request, h),
                WalletResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertNotNull(response.getBody());
        assertThat(response.getBody().balance()).isEqualByComparingTo(amount);
    }

    @Test
    void shouldReturn404WhenDepositingOnNonExistingWallet() {
        // Arrange
        UUID id = UUID.randomUUID();
        DepositRequest request = new DepositRequest(BigDecimal.ONE);

        // Act
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl() + "/" + id + "/deposit",
                request,
                ErrorResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertNotNull(response.getBody());
        assertThat(response.getBody().message()).isEqualTo("Wallet not found");
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

    // region ─ withdraw ───────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldWithdrawAndReturn200() {
        // Arrange
        CreateWalletRequest createRequest = new CreateWalletRequest(globalUserId);
        String walletUrl = Objects.requireNonNull(
                restTemplate.postForEntity(baseUrl(), createRequest, WalletResponse.class)
                        .getHeaders().getLocation()
        ).toString();
        restTemplate.postForEntity(walletUrl + "/deposit", new DepositRequest(BigDecimal.TWO), WalletResponse.class);
        BigDecimal amount = BigDecimal.ONE;
        WithdrawRequest request = new WithdrawRequest(amount);

        // Act
        ResponseEntity<WalletResponse> response = restTemplate.postForEntity(
                walletUrl + "/withdraw", request, WalletResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertNotNull(response.getBody());
        assertThat(response.getBody().balance()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void shouldReturn404WhenWithdrawingOnNonExistingWallet() {
        // Arrange
        UUID bad = UUID.randomUUID();
        WithdrawRequest request = new WithdrawRequest(BigDecimal.ONE);

        // Act
        ResponseEntity<ErrorResponse> resp = restTemplate.postForEntity(
                baseUrl() + "/" + bad + "/withdraw",
                request,
                ErrorResponse.class
        );

        // Assert
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertNotNull(resp.getBody());
        assertThat(resp.getBody().message()).isEqualTo("Wallet not found");
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

    // region ─ transfer ───────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void shouldTransferAndReturn200() {
        // Arrange
        CreateWalletRequest c = new CreateWalletRequest(globalUserId);
        String url1 = Objects.requireNonNull(
                restTemplate.postForEntity(baseUrl(), c, WalletResponse.class)
                        .getHeaders().getLocation()
        ).toString();
        String url2 = Objects.requireNonNull(
                restTemplate.postForEntity(baseUrl(), c, WalletResponse.class)
                        .getHeaders().getLocation()
        ).toString();
        BigDecimal amount = BigDecimal.ONE;
        UUID targetWalletId = UUID.fromString(url2.substring(url2.lastIndexOf("/") + 1));
        restTemplate.postForEntity(url1 + "/deposit", new DepositRequest(BigDecimal.TWO), WalletResponse.class);
        TransferRequest request = new TransferRequest(targetWalletId, amount);

        // Act
        ResponseEntity<WalletResponse> response = restTemplate.postForEntity(
                url1 + "/transfer", request, WalletResponse.class
        );
        ResponseEntity<WalletResponse> t2 = restTemplate.getForEntity(url2, WalletResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertNotNull(response.getBody());
        assertThat(response.getBody().balance()).isEqualByComparingTo(BigDecimal.ONE);
        assertNotNull(t2.getBody());
        assertThat(t2.getBody().balance()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void shouldReturn404WhenTransferringOnNonExistingWallet() {
        UUID bad = UUID.randomUUID();
        TransferRequest tr = new TransferRequest(UUID.randomUUID(), BigDecimal.ONE);
        ResponseEntity<ErrorResponse> resp = restTemplate.postForEntity(
                baseUrl() + "/" + bad + "/transfer",
                tr,
                ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertNotNull(resp.getBody());
        assertThat(resp.getBody().message()).isEqualTo("Source Wallet not found");
    }

    // endregion ───────────────────────────────────────────────────────────────────────────────────────────────────────

}