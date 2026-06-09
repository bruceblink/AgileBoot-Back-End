package app.keystone.domain.system.user.keylo;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.jackson.JacksonUtil;
import app.keystone.domain.system.user.command.AddUserCommand;
import java.net.URLEncoder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeyloUserProvisioningService {

    private final KeyloUserProvisioningProperties properties;

    public KeyloUserProvisioningResult provisionUser(AddUserCommand command) {
        if (!properties.isEnabled()) {
            return null;
        }

        if (StringUtils.isBlank(properties.getCreateUserUrl())
            || StringUtils.isBlank(properties.getAdminTokenUrl())
            || StringUtils.isBlank(properties.getAdminClientId())
            || StringUtils.isBlank(properties.getAdminClientSecret())) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }

        Map<String, Object> body = new HashMap<>();
        body.put(properties.getUsernameField(), command.getUsername());
        body.put(properties.getNicknameField(), command.getNickname());
        body.put(properties.getEmailField(), command.getEmail());
        body.put(properties.getPhoneField(), command.getPhoneNumber());
        body.put(properties.getPasswordField(), command.getPassword());

        try {
            String adminAccessToken = getAdminAccessToken();
            HttpResponse<String> response = sendPostJson(
                properties.getCreateUserUrl(),
                JacksonUtil.to(body),
                Map.of(properties.getAuthHeaderName(), "Bearer " + adminAccessToken),
                properties.getTimeoutMillis()
            );

            int statusCode = response.statusCode();
            String responseBody = response.body();
            if (statusCode < 200 || statusCode >= 300) {
                log.error("Keylo user provisioning failed, status={}, response={}", statusCode, responseBody);
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, "HTTP " + statusCode);
            }

            String error = JacksonUtil.getAsString(responseBody, "error");
            if (StringUtils.isNotBlank(error)) {
                String message = JacksonUtil.getAsString(responseBody, "message");
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED,
                    StringUtils.defaultIfBlank(message, error));
            }

            String keyloSubject = extractField(responseBody, properties.getSubjectField());
            if (StringUtils.isBlank(keyloSubject)) {
                keyloSubject = resolveSubject(command);
            }
            if (StringUtils.isBlank(keyloSubject)) {
                log.error("Keylo user provisioning succeeded but subject missing, response={}", responseBody);
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING);
            }
            String keyloUserId = extractField(responseBody, properties.getUserIdField());
            if (StringUtils.isBlank(keyloUserId)) {
                log.error("Keylo user provisioning succeeded but user id missing, response={}", responseBody);
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, "Keylo user id missing");
            }
            return new KeyloUserProvisioningResult(keyloSubject, keyloUserId);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, e.getMessage());
        }
    }

    public void resetPassword(String keyloUserId, String newPassword) {
        if (!properties.isEnabled()) {
            return;
        }
        if (StringUtils.isBlank(keyloUserId)) {
            log.error("Keylo password reset failed because external user id is missing.");
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, "Keylo user id missing");
        }

        if (StringUtils.isBlank(properties.getResetPasswordUrlTemplate())) {
            log.error("Keylo password reset failed because reset-password URL is not configured, keyloUserId={}",
                keyloUserId);
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }

        if (StringUtils.isBlank(properties.getAdminTokenUrl())
            || StringUtils.isBlank(properties.getAdminClientId())
            || StringUtils.isBlank(properties.getAdminClientSecret())) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }

        Map<String, Object> body = new HashMap<>();
        body.put(properties.getPasswordField(), newPassword);

        try {
            String adminAccessToken = getAdminAccessToken();
            HttpResponse<String> response = sendPostJson(
                resetPasswordUrl(keyloUserId),
                JacksonUtil.to(body),
                Map.of(properties.getAuthHeaderName(), "Bearer " + adminAccessToken),
                properties.getTimeoutMillis()
            );

            int statusCode = response.statusCode();
            String responseBody = response.body();
            if (statusCode < 200 || statusCode >= 300) {
                log.error("Keylo password reset failed, userId={}, status={}, response={}", keyloUserId,
                    statusCode, responseBody);
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, "HTTP " + statusCode);
            }

            String error = JacksonUtil.getAsString(responseBody, "error");
            if (StringUtils.isNotBlank(error)) {
                String message = JacksonUtil.getAsString(responseBody, "message");
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED,
                    StringUtils.defaultIfBlank(message, error));
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, e.getMessage());
        }
    }

    private String getAdminAccessToken() {
        Map<String, Object> tokenBody = new HashMap<>();
        tokenBody.put("client_id", properties.getAdminClientId());
        tokenBody.put("client_secret", properties.getAdminClientSecret());

        HttpResponse<String> tokenResponse = sendPostJson(
            properties.getAdminTokenUrl(),
            JacksonUtil.to(tokenBody),
            Map.of(),
            properties.getTimeoutMillis()
        );

        if (tokenResponse.statusCode() < 200 || tokenResponse.statusCode() >= 300) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED,
                "admin token HTTP " + tokenResponse.statusCode());
        }

        String tokenResponseBody = tokenResponse.body();
        String adminAccessToken = JacksonUtil.getAsString(tokenResponseBody, "access_token");
        if (StringUtils.isBlank(adminAccessToken)) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, "admin access_token missing");
        }
        return adminAccessToken;
    }

    protected HttpResponse<String> sendPostJson(String url, String jsonBody, Map<String, String> headers, int timeoutMillis) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMillis))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
            return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, e.getMessage());
        }
    }

    private String extractField(String responseBody, String fieldName) {
        if (StringUtils.isBlank(fieldName)) {
            return null;
        }

        String dataNode = JacksonUtil.getAsString(responseBody, "data");
        String userNode = JacksonUtil.getAsString(dataNode, "user");

        String value = JacksonUtil.getAsString(userNode, fieldName);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        value = JacksonUtil.getAsString(dataNode, fieldName);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        return JacksonUtil.getAsString(responseBody, fieldName);
    }

    private String resetPasswordUrl(String keyloUserId) {
        return properties.getResetPasswordUrlTemplate()
            .replace("{userId}", URLEncoder.encode(keyloUserId, StandardCharsets.UTF_8))
            .replace("{user_id}", URLEncoder.encode(keyloUserId, StandardCharsets.UTF_8));
    }

    private String resolveSubject(AddUserCommand command) {
        if (StringUtils.isBlank(properties.getSubjectTemplate()) || command == null) {
            return null;
        }
        return properties.getSubjectTemplate()
            .replace("{username}", StringUtils.defaultString(command.getUsername()))
            .replace("{email}", StringUtils.defaultString(command.getEmail()));
    }
}
