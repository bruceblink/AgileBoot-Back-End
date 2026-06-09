package app.keystone.admin.customize.service.login.keylo;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.jackson.JacksonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeyloCredentialVerifier {

    private final KeyloProperties keyloProperties;

    private final KeyloTokenVerifier keyloTokenVerifier;

    public KeyloTokenIdentity verify(String username, String password) {
        if (StringUtils.isBlank(keyloProperties.getCredentialVerifyUrl())) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }

        Map<String, Object> body = new HashMap<>();
        body.put(resolveCredentialUsernameField(), username);
        body.put(resolveCredentialPasswordField(), password);

        try {
            Map<String, String> tokenHeaders = new HashMap<>();
            if (StringUtils.isNotBlank(keyloProperties.getCredentialAuthHeaderValue())) {
                tokenHeaders.put(resolveCredentialAuthHeaderName(), keyloProperties.getCredentialAuthHeaderValue());
            }
            HttpResponse<String> tokenResponse = sendPostJson(
                keyloProperties.getCredentialVerifyUrl(),
                JacksonUtil.to(body),
                tokenHeaders
            );
            if (tokenResponse.statusCode() < 200 || tokenResponse.statusCode() >= 300) {
                handleTokenResponseFailure(tokenResponse);
            }

            String tokenResponseBody = tokenResponse.body();
            String accessToken = JacksonUtil.getAsString(tokenResponseBody, "access_token");
            if (StringUtils.isBlank(accessToken)) {
                log.warn("Keylo credential verification returned no access_token. response={}", tokenResponseBody);
                throw new ApiException(ErrorCode.Business.LOGIN_ERROR, "认证服务异常");
            }

            KeyloTokenIdentity verifiedIdentity = keyloTokenVerifier.verify(accessToken);
            JsonNode expiresInNode = JacksonUtil.getAsJsonObject(tokenResponseBody, "expires_in");
            Long expiresIn = expiresInNode == null || expiresInNode.isNull() ? null : JacksonUtil.getAsLong(tokenResponseBody, "expires_in");
            return new KeyloTokenIdentity(
                verifiedIdentity.getKeyloSubject(),
                verifiedIdentity.getKeyloUserId(),
                accessToken,
                JacksonUtil.getAsString(tokenResponseBody, "refresh_token"),
                expiresIn == null ? verifiedIdentity.getExpiresIn() : expiresIn,
                StringUtils.defaultIfBlank(JacksonUtil.getAsString(tokenResponseBody, "token_type"), verifiedIdentity.getTokenType())
            );
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Keylo credential verification request failed", e);
            throw new ApiException(e, ErrorCode.Business.LOGIN_ERROR, "认证服务异常");
        }
    }

    private void handleTokenResponseFailure(HttpResponse<String> tokenResponse) {
        int statusCode = tokenResponse.statusCode();
        log.warn("Keylo credential verification failed. status={}, response={}", statusCode, tokenResponse.body());
        if (statusCode == 400 || statusCode == 401 || statusCode == 403) {
            throw new ApiException(ErrorCode.Business.LOGIN_WRONG_USER_PASSWORD);
        }
        throw new ApiException(ErrorCode.Business.LOGIN_ERROR, "认证服务异常");
    }

    private HttpResponse<String> sendPostJson(String url, String jsonBody, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMillis(10000))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String resolveCredentialAuthHeaderName() {
        return StringUtils.isNotBlank(keyloProperties.getCredentialAuthHeaderName())
            ? keyloProperties.getCredentialAuthHeaderName() : "Authorization";
    }

    private String resolveCredentialUsernameField() {
        return StringUtils.isNotBlank(keyloProperties.getCredentialUsernameField())
            ? keyloProperties.getCredentialUsernameField() : "username";
    }

    private String resolveCredentialPasswordField() {
        return StringUtils.isNotBlank(keyloProperties.getCredentialPasswordField())
            ? keyloProperties.getCredentialPasswordField() : "password";
    }
}
