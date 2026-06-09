package app.keystone.admin.customize.service.login.keylo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.test.util.ReflectionTestUtils;

class KeyloTokenVerifierTest {

    private final KeyloProperties keyloProperties = new KeyloProperties();
    private final KeyloTokenVerifier keyloTokenVerifier = new KeyloTokenVerifier(keyloProperties);

    @Test
    void verify_shouldThrowConfigMissing_whenIssuerEmpty() {
        keyloProperties.setIssuerUri(null);
        keyloProperties.setJwkSetUri("http://localhost/.well-known/jwks.json");

        ApiException exception = assertThrows(ApiException.class, () -> keyloTokenVerifier.verify("mock-token"));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING, exception.getErrorCode());
    }

    @Test
    void verify_shouldThrowInvalidToken_whenTokenMalformed() {
        keyloProperties.setIssuerUri("http://localhost/mock-issuer");
        keyloProperties.setJwkSetUri("http://localhost/.well-known/jwks.json");

        ApiException exception = assertThrows(ApiException.class, () -> keyloTokenVerifier.verify("not-a-jwt"));

        assertEquals(ErrorCode.Client.INVALID_TOKEN, exception.getErrorCode());
    }

    @Test
    void verify_shouldThrowSubjectMissing_whenSubjectClaimEmpty() {
        keyloProperties.setIssuerUri("http://localhost/mock-issuer");
        keyloProperties.setJwkSetUri(null);
        keyloProperties.setSubjectClaim("sub");

        JwtDecoder jwtDecoder = mock(JwtDecoder.class);
        Jwt jwt = mock(Jwt.class);
        when(jwtDecoder.decode("mock-token")).thenReturn(jwt);
        when(jwt.getClaimAsString("sub")).thenReturn("");

        try (MockedStatic<JwtDecoders> jwtDecodersMocked = mockStatic(JwtDecoders.class)) {
            jwtDecodersMocked.when(() -> JwtDecoders.fromIssuerLocation("http://localhost/mock-issuer"))
                .thenReturn(jwtDecoder);

            ApiException exception = assertThrows(ApiException.class, () -> keyloTokenVerifier.verify("mock-token"));

            assertEquals(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING, exception.getErrorCode());
        }
    }

    @Test
    void verify_shouldReturnKeyloTokenIdentity_whenClaimsValid() {
        keyloProperties.setIssuerUri("http://localhost/mock-issuer");
        keyloProperties.setJwkSetUri(null);
        keyloProperties.setSubjectClaim("sub");
        keyloProperties.setUserIdClaim("uid");

        JwtDecoder jwtDecoder = mock(JwtDecoder.class);
        Jwt jwt = mock(Jwt.class);
        when(jwtDecoder.decode("mock-token")).thenReturn(jwt);
        when(jwt.getClaimAsString("sub")).thenReturn("user:admin");
        when(jwt.getClaimAsString("uid")).thenReturn("uid-001");
        when(jwt.getClaimAsString("token_type")).thenReturn("access");
        when(jwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(120));

        try (MockedStatic<JwtDecoders> jwtDecodersMocked = mockStatic(JwtDecoders.class)) {
            jwtDecodersMocked.when(() -> JwtDecoders.fromIssuerLocation("http://localhost/mock-issuer"))
                .thenReturn(jwtDecoder);

            KeyloTokenIdentity identity = keyloTokenVerifier.verify("mock-token");

            assertEquals("user:admin", identity.getKeyloSubject());
            assertEquals("uid-001", identity.getKeyloUserId());
            assertEquals("mock-token", identity.getAccessToken());
            assertEquals("access", identity.getTokenType());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildValidator_shouldAcceptAnyConfiguredAudience() {
        keyloProperties.setIssuerUri("http://localhost/mock-issuer");
        keyloProperties.setAudiences(List.of("admin-backend", "keystone-admin"));

        OAuth2TokenValidator<Jwt> validator = (OAuth2TokenValidator<Jwt>) ReflectionTestUtils
            .invokeMethod(keyloTokenVerifier, "buildValidator");
        assertNotNull(validator);

        OAuth2TokenValidatorResult validResult = validator.validate(jwtWithAudience("keystone-admin"));
        OAuth2TokenValidatorResult invalidResult = validator.validate(jwtWithAudience("unknown-service"));

        assertFalse(validResult.hasErrors());
        assertTrue(invalidResult.hasErrors());
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildValidator_shouldAcceptTrustedNonUrlIssuer() {
        keyloProperties.setIssuerUri("http://localhost/mock-issuer");
        keyloProperties.setTrustedIssuers(List.of("keylo"));
        keyloProperties.setAudiences(List.of("admin-backend"));

        OAuth2TokenValidator<Jwt> validator = (OAuth2TokenValidator<Jwt>) ReflectionTestUtils
            .invokeMethod(keyloTokenVerifier, "buildValidator");

        Jwt jwt = Jwt.withTokenValue("mock-token")
            .header("alg", "none")
            .issuer("keylo")
            .claim("aud", List.of("admin-backend"))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(120))
            .build();

        assertFalse(validator.validate(jwt).hasErrors());
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildValidator_shouldAcceptStringAudienceClaim() {
        keyloProperties.setTrustedIssuers(List.of("keylo"));
        keyloProperties.setAudiences(List.of("admin-backend"));

        OAuth2TokenValidator<Jwt> validator = (OAuth2TokenValidator<Jwt>) ReflectionTestUtils
            .invokeMethod(keyloTokenVerifier, "buildValidator");

        Jwt jwt = Jwt.withTokenValue("mock-token")
            .header("alg", "none")
            .claim("iss", "keylo")
            .claim("aud", "admin-backend")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(120))
            .build();

        assertFalse(validator.validate(jwt).hasErrors());
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildValidator_shouldRejectExpiredTokenBeyondConfiguredClockSkew() {
        keyloProperties.setIssuerUri("http://localhost/mock-issuer");
        keyloProperties.setAudiences(List.of());
        keyloProperties.setClockSkewSeconds(5);

        OAuth2TokenValidator<Jwt> validator = (OAuth2TokenValidator<Jwt>) ReflectionTestUtils
            .invokeMethod(keyloTokenVerifier, "buildValidator");
        Jwt jwt = Jwt.withTokenValue("mock-token")
            .header("alg", "none")
            .issuer("http://localhost/mock-issuer")
            .subject("user:admin")
            .issuedAt(Instant.now().minusSeconds(120))
            .expiresAt(Instant.now().minusSeconds(10))
            .build();

        assertTrue(validator.validate(jwt).hasErrors());
    }

    private Jwt jwtWithAudience(String audience) {
        return Jwt.withTokenValue("mock-token")
            .header("alg", "none")
            .issuer("http://localhost/mock-issuer")
            .claim("aud", List.of(audience))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(120))
            .build();
    }
}
