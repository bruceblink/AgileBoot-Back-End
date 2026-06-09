package app.keystone.admin.customize.service.login.keylo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.jackson.JacksonUtil;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeyloCredentialVerifierTest {

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void verify_shouldReturnNullExpiresIn_whenTokenResponseDoesNotContainExpiresIn() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();

        server.createContext("/token", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] responseBody = "{\"access_token\":\"keylo-access-token\",\"refresh_token\":\"keylo-refresh-token\",\"token_type\":\"Bearer\"}"
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBody.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }
        });
        server.start();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        KeyloProperties properties = new KeyloProperties();
        properties.setCredentialVerifyUrl(baseUrl + "/token");
        properties.setCredentialUsernameField("username");
        properties.setCredentialPasswordField("password");
        KeyloTokenVerifier keyloTokenVerifier = mock(KeyloTokenVerifier.class);
        when(keyloTokenVerifier.verify("keylo-access-token"))
            .thenReturn(new KeyloTokenIdentity("user:admin", "uid-001", "keylo-access-token", null, null, "access"));

        KeyloCredentialVerifier verifier = new KeyloCredentialVerifier(properties, keyloTokenVerifier);

        KeyloTokenIdentity identity = verifier.verify("admin", "plain-password");

        assertEquals("user:admin", identity.getKeyloSubject());
        assertEquals("uid-001", identity.getKeyloUserId());
        assertEquals("keylo-access-token", identity.getAccessToken());
        assertEquals("keylo-refresh-token", identity.getRefreshToken());
        assertNull(identity.getExpiresIn());
        assertEquals("Bearer", identity.getTokenType());
        verify(keyloTokenVerifier).verify("keylo-access-token");
        assertEquals("admin", JacksonUtil.getAsString(requestBody.get(), "username"));
        assertEquals("plain-password", JacksonUtil.getAsString(requestBody.get(), "password"));
    }

    @Test
    void verify_shouldFallbackToDefaultClaims_whenConfiguredClaimsBlank() throws Exception {
        AtomicReference<String> customAuthHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();

        server.createContext("/token", exchange -> {
            customAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] responseBody = "{\"access_token\":\"keylo-access-token\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBody.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }
        });
        server.start();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        KeyloProperties properties = new KeyloProperties();
        properties.setCredentialVerifyUrl(baseUrl + "/token");
        properties.setSubjectClaim(" ");
        properties.setUserIdClaim("");
        properties.setCredentialAuthHeaderName("");
        properties.setCredentialAuthHeaderValue("Basic test-secret");
        properties.setCredentialUsernameField("");
        properties.setCredentialPasswordField(" ");
        KeyloTokenVerifier keyloTokenVerifier = mock(KeyloTokenVerifier.class);
        when(keyloTokenVerifier.verify("keylo-access-token"))
            .thenReturn(new KeyloTokenIdentity("user:admin", "uid-001", "keylo-access-token", null, null, "access"));

        KeyloCredentialVerifier verifier = new KeyloCredentialVerifier(properties, keyloTokenVerifier);

        KeyloTokenIdentity identity = verifier.verify("admin", "plain-password");

        assertEquals("user:admin", identity.getKeyloSubject());
        assertEquals("uid-001", identity.getKeyloUserId());
        assertEquals("Basic test-secret", customAuthHeader.get());
        assertEquals("admin", JacksonUtil.getAsString(requestBody.get(), "username"));
        assertEquals("plain-password", JacksonUtil.getAsString(requestBody.get(), "password"));
    }

    @Test
    void verify_shouldWrapBadCredentialsAsKeystoneLoginFailure() throws Exception {
        server.createContext("/token", exchange -> {
            byte[] responseBody = "{\"error\":\"invalid_grant\",\"message\":\"invalid username or password\"}"
                .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, responseBody.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }
        });
        server.start();

        KeyloCredentialVerifier verifier = verifierFor("/token");

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("admin", "wrong-password"));

        assertEquals(ErrorCode.Business.LOGIN_WRONG_USER_PASSWORD, exception.getErrorCode());
    }

    @Test
    void verify_shouldWrapKeyloServerErrorAsKeystoneLoginError() throws Exception {
        server.createContext("/token", exchange -> {
            byte[] responseBody = "{\"error\":\"server_error\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, responseBody.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }
        });
        server.start();

        KeyloCredentialVerifier verifier = verifierFor("/token");

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("admin", "plain-password"));

        assertEquals(ErrorCode.Business.LOGIN_ERROR, exception.getErrorCode());
        assertEquals("登录失败：认证服务异常", exception.getMessage());
    }

    @Test
    void verify_shouldWrapMalformedTokenResponseAsKeystoneLoginError() throws Exception {
        server.createContext("/token", exchange -> {
            byte[] responseBody = "{\"token_type\":\"Bearer\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBody.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }
        });
        server.start();

        KeyloCredentialVerifier verifier = verifierFor("/token");

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("admin", "plain-password"));

        assertEquals(ErrorCode.Business.LOGIN_ERROR, exception.getErrorCode());
        assertEquals("登录失败：认证服务异常", exception.getMessage());
    }

    private KeyloCredentialVerifier verifierFor(String path) {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        KeyloProperties properties = new KeyloProperties();
        properties.setCredentialVerifyUrl(baseUrl + path);
        return new KeyloCredentialVerifier(properties, mock(KeyloTokenVerifier.class));
    }
}
