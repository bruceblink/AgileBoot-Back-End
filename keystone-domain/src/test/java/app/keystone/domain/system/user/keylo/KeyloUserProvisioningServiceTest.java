package app.keystone.domain.system.user.keylo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.jackson.JacksonUtil;
import app.keystone.domain.system.user.command.AddUserCommand;
import java.net.http.HttpResponse;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class KeyloUserProvisioningServiceTest {

    @Test
    void provisionUser_shouldReturnNull_whenProvisioningDisabled() {
        KeyloUserProvisioningProperties properties = new KeyloUserProvisioningProperties();
        properties.setEnabled(false);

        KeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);

        KeyloUserProvisioningResult result = service.provisionUser(buildCommand());

        assertNull(result);
    }

    @Test
    void provisionUser_shouldThrowConfigMissing_whenRequiredConfigMissing() {
        KeyloUserProvisioningProperties properties = new KeyloUserProvisioningProperties();
        properties.setEnabled(true);
        properties.setCreateUserUrl("http://keylo.local/v1/admin/users");
        properties.setAdminTokenUrl("http://keylo.local/v1/admin/token");
        properties.setAdminClientId("cli-admin-root");
        properties.setAdminClientSecret("");

        KeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldThrowProvisionFailed_whenAdminTokenHttpStatusNot2xx() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(401, "{}"));

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldThrowProvisionFailed_whenAdminTokenMissing() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(200, "{\"token_type\":\"Bearer\"}"));

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldThrowProvisionFailed_whenCreateUserHttpStatusNot2xx() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(500, "{\"message\":\"failed\"}"));

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldThrowProvisionFailed_whenCreateUserReturnsBusinessError() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(200, "{\"error\":\"invalid_token\",\"message\":\"Invalid token\"}"));

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldThrowSubjectMissing_whenSubjectFieldAbsent() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        properties.setSubjectTemplate("");
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(200, "{\"data\":{\"user\":{\"username\":\"u-1\"}}}"));

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldThrowProvisionFailed_whenUserIdFieldAbsent() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(201, "{\"data\":{\"sub\":\"user:keystone-user\",\"username\":\"keystone-user\"}}"));

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldReturnExternalIdentity_whenProvisioningSucceededWithDataNode() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(201, "{\"data\":{\"sub\":\"sub-2002\",\"id\":\"uid-2002\"}}"));

        KeyloUserProvisioningResult result = service.provisionUser(buildCommand());

        assertEquals("sub-2002", result.getKeyloSubject());
        assertEquals("uid-2002", result.getKeyloUserId());
    }

    @Test
    void resetPassword_shouldPostPasswordToConfiguredKeyloEndpoint() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(200, "{\"success\":true}"));

        service.resetPassword("uid-2002", "NewPassword123!");

        assertEquals(properties.getAdminTokenUrl(), service.requests.get(0).url());
        RecordedRequest resetRequest = service.requests.get(1);
        assertEquals("http://keylo.local/v1/admin/users/uid-2002/reset-password", resetRequest.url());
        assertEquals("NewPassword123!", JacksonUtil.getAsString(resetRequest.jsonBody(), "password"));
        assertEquals("Bearer admin-token", resetRequest.headers().get("Authorization"));
    }

    @Test
    void resetPassword_shouldSkip_whenProvisioningDisabled() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        properties.setEnabled(false);
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);

        service.resetPassword("uid-2002", "NewPassword123!");

        assertEquals(0, service.requests.size());
    }

    @Test
    void resetPassword_shouldSkip_whenResetPasswordUrlMissing() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        properties.setResetPasswordUrlTemplate("");
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);

        service.resetPassword("uid-2002", "NewPassword123!");

        assertEquals(0, service.requests.size());
    }

    @Test
    void provisionUser_shouldDeriveSubject_whenCreateUserResponseOnlyContainsUserId() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(201, "{\"data\":{\"id\":\"uid-2003\",\"username\":\"keystone-user\"}}"));

        KeyloUserProvisioningResult result = service.provisionUser(buildCommand());

        assertEquals("user:keystone-user", result.getKeyloSubject());
        assertEquals("uid-2003", result.getKeyloUserId());
    }

    private HttpResponse<String> response(int statusCode, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }

    private KeyloUserProvisioningProperties buildDefaultProperties() {
        KeyloUserProvisioningProperties properties = new KeyloUserProvisioningProperties();
        properties.setEnabled(true);
        properties.setCreateUserUrl("http://keylo.local/v1/admin/users");
        properties.setResetPasswordUrlTemplate("http://keylo.local/v1/admin/users/{userId}/reset-password");
        properties.setAdminTokenUrl("http://keylo.local/v1/admin/token");
        properties.setAdminClientId("cli-admin-root");
        properties.setAdminClientSecret("strong-secret");
        properties.setAuthHeaderName("Authorization");
        properties.setSubjectField("sub");
        properties.setSubjectTemplate("user:{username}");
        properties.setUserIdField("id");
        properties.setUsernameField("username");
        properties.setNicknameField("nickname");
        properties.setEmailField("email");
        properties.setPhoneField("phoneNumber");
        properties.setPasswordField("password");
        return properties;
    }

    private AddUserCommand buildCommand() {
        AddUserCommand command = new AddUserCommand();
        command.setUsername("keystone-user");
        command.setNickname("Keystone User");
        command.setEmail("user@test.com");
        command.setPhoneNumber("13800000000");
        command.setPassword("Keylo#123456");
        return command;
    }

    private static class TestableKeyloUserProvisioningService extends KeyloUserProvisioningService {
        private final Deque<HttpResponse<String>> responses = new ArrayDeque<>();
        private final List<RecordedRequest> requests = new ArrayList<>();

        private TestableKeyloUserProvisioningService(KeyloUserProvisioningProperties properties) {
            super(properties);
        }

        void enqueue(HttpResponse<String> response) {
            responses.addLast(response);
        }

        @Override
        protected HttpResponse<String> sendPostJson(String url, String jsonBody, Map<String, String> headers, int timeoutMillis) {
            requests.add(new RecordedRequest(url, jsonBody, headers));
            HttpResponse<String> response = responses.pollFirst();
            if (response == null) {
                throw new IllegalStateException("No mocked response for " + url);
            }
            return response;
        }
    }

    private record RecordedRequest(String url, String jsonBody, Map<String, String> headers) {
    }
}
