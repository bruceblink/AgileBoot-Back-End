package app.keystone.admin.customize.service.login.keylo;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.jackson.JacksonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Keylo token verifier.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeyloTokenVerifier {

    private static final String DEFAULT_SUBJECT_CLAIM = "sub";
    private static final String DEFAULT_USER_ID_CLAIM = "uid";
    private static final String ISSUER_CLAIM = "iss";
    private static final String AUDIENCE_CLAIM = "aud";
    private static final String KEY_ID_HEADER = "kid";
    private static final String TOKEN_TYPE_CLAIM = "token_type";

    private final KeyloProperties keyloProperties;

    private volatile JwtDecoder jwtDecoder;

    public KeyloTokenIdentity verify(String accessToken) {
        try {
            Jwt jwt = buildJwtDecoder().decode(accessToken);
            return buildIdentity(jwt, accessToken);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            logValidationFailure(accessToken, e);
            throw new ApiException(e, ErrorCode.Client.INVALID_TOKEN);
        }
    }

    private JwtDecoder buildJwtDecoder() {
        JwtDecoder cachedJwtDecoder = jwtDecoder;
        if (cachedJwtDecoder != null) {
            return cachedJwtDecoder;
        }

        synchronized (this) {
            if (jwtDecoder == null) {
                jwtDecoder = createJwtDecoder();
            }
            return jwtDecoder;
        }
    }

    private JwtDecoder createJwtDecoder() {
        List<String> trustedIssuers = resolveTrustedIssuers();
        if (trustedIssuers.isEmpty()) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }

        if (StringUtils.hasText(keyloProperties.getJwkSetUri())) {
            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(keyloProperties.getJwkSetUri()).build();
            jwtDecoder.setJwtValidator(buildValidator());
            return jwtDecoder;
        }

        if (!StringUtils.hasText(keyloProperties.getIssuerUri())) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }
        JwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(keyloProperties.getIssuerUri());
        if (jwtDecoder instanceof NimbusJwtDecoder nimbusJwtDecoder) {
            nimbusJwtDecoder.setJwtValidator(buildValidator());
        }
        return jwtDecoder;
    }

    private OAuth2TokenValidator<Jwt> buildValidator() {
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(clockSkewSeconds()));
        OAuth2TokenValidator<Jwt> issuerValidator = buildIssuerValidator(resolveTrustedIssuers());
        OAuth2TokenValidator<Jwt> defaultValidator = new DelegatingOAuth2TokenValidator<>(timestampValidator, issuerValidator);

        List<String> trustedAudiences = normalize(keyloProperties.getAudiences());
        if (trustedAudiences.isEmpty()) {
            return defaultValidator;
        }

        OAuth2TokenValidator<Jwt> audienceValidator = buildAudienceValidator(trustedAudiences);
        return new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator);
    }

    private OAuth2TokenValidator<Jwt> buildIssuerValidator(List<String> trustedIssuers) {
        return token -> {
            String issuer = token.getClaimAsString(ISSUER_CLAIM);
            if (contains(trustedIssuers, issuer)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "The iss claim is not trusted", null));
        };
    }

    private OAuth2TokenValidator<Jwt> buildAudienceValidator(List<String> trustedAudiences) {
        return token -> {
            List<String> audiences = token.getAudience();
            if (audiences != null && audiences.stream().anyMatch(trustedAudiences::contains)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "The required audience is missing", null));
        };
    }

    private KeyloTokenIdentity buildIdentity(Jwt jwt, String accessToken) {
        String subject = jwt.getClaimAsString(resolveClaimName(keyloProperties.getSubjectClaim(), DEFAULT_SUBJECT_CLAIM));
        if (!StringUtils.hasText(subject)) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING);
        }
        String userId = jwt.getClaimAsString(resolveClaimName(keyloProperties.getUserIdClaim(), DEFAULT_USER_ID_CLAIM));
        return new KeyloTokenIdentity(subject, userId, accessToken, null, expiresIn(jwt), jwt.getClaimAsString(TOKEN_TYPE_CLAIM));
    }

    private Long expiresIn(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();
        return expiresAt == null
            ? null
            : Math.max(0L, (expiresAt.toEpochMilli() - System.currentTimeMillis()) / 1000L);
    }

    private long clockSkewSeconds() {
        Integer clockSkewSeconds = keyloProperties.getClockSkewSeconds();
        return clockSkewSeconds == null || clockSkewSeconds < 0 ? 60L : clockSkewSeconds;
    }

    private List<String> resolveTrustedIssuers() {
        List<String> trustedIssuers = normalize(keyloProperties.getTrustedIssuers());
        if (trustedIssuers.isEmpty() && StringUtils.hasText(keyloProperties.getIssuerUri())) {
            trustedIssuers.add(keyloProperties.getIssuerUri().trim());
        }
        return trustedIssuers;
    }

    private List<String> normalize(Collection<String> values) {
        List<String> normalizedValues = new ArrayList<>();
        if (values != null) {
            normalizedValues.addAll(values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList());
        }
        return normalizedValues;
    }

    private boolean contains(List<String> trustedValues, String value) {
        return StringUtils.hasText(value) && trustedValues.stream().anyMatch(trustedValue -> Objects.equals(trustedValue, value));
    }

    private String resolveClaimName(String configuredClaimName, String defaultClaimName) {
        return StringUtils.hasText(configuredClaimName) ? configuredClaimName : defaultClaimName;
    }

    private void logValidationFailure(String accessToken, Exception exception) {
        KeyloTokenDiagnostics diagnostics = inspectToken(accessToken);
        log.warn("Keylo token validation failed. configuredIssuer={}, trustedIssuers={}, jwkSetUri={}, tokenIssuer={}, tokenAudience={}, tokenKid={}, cause={}",
            keyloProperties.getIssuerUri(),
            resolveTrustedIssuers(),
            keyloProperties.getJwkSetUri(),
            diagnostics.issuer(),
            diagnostics.audience(),
            diagnostics.keyId(),
            exception.getMessage());
    }

    private KeyloTokenDiagnostics inspectToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return KeyloTokenDiagnostics.empty();
        }
        String[] tokenParts = accessToken.split("\\.");
        if (tokenParts.length < 2) {
            return KeyloTokenDiagnostics.empty();
        }
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(tokenParts[0]), StandardCharsets.UTF_8);
            String payloadJson = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
            return new KeyloTokenDiagnostics(
                safeJsonString(payloadJson, ISSUER_CLAIM),
                safeJsonString(payloadJson, AUDIENCE_CLAIM),
                safeJsonString(headerJson, KEY_ID_HEADER)
            );
        } catch (Exception ignored) {
            return KeyloTokenDiagnostics.empty();
        }
    }

    private String safeJsonString(String json, String key) {
        JsonNode node = JacksonUtil.getAsJsonObject(json, key);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        return node.toString();
    }

    private record KeyloTokenDiagnostics(String issuer, String audience, String keyId) {

        private static KeyloTokenDiagnostics empty() {
            return new KeyloTokenDiagnostics(null, null, null);
        }
    }
}
