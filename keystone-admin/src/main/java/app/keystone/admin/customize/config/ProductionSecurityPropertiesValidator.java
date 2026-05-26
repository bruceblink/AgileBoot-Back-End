package app.keystone.admin.customize.config;

import app.keystone.admin.customize.service.login.keylo.KeyloProperties;
import app.keystone.domain.system.user.keylo.KeyloUserProvisioningProperties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fails fast when production starts with missing or unsafe security settings.
 */
@Component
@RequiredArgsConstructor
public class ProductionSecurityPropertiesValidator implements ApplicationRunner {

    private static final String DEFAULT_TOKEN_SECRET = "sdhfkjshBN6rr32df38";
    private static final String DOCKER_PLACEHOLDER_TOKEN_SECRET = "change-me-please-update-token-secret-32bytes";
    private static final String DEFAULT_KEYLO_ADMIN_CLIENT_SECRET = "replace-with-strong-admin-secret";
    private static final String DEFAULT_RSA_PRIVATE_KEY_PREFIX = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8";

    private final Environment environment;

    private final KeyloProperties keyloProperties;

    private final KeyloUserProvisioningProperties keyloProvisioningProperties;

    @Value("${token.secret:}")
    private String tokenSecret;

    @Value("${keystone.rsaPrivateKey:}")
    private String rsaPrivateKey;

    @Value("${keystone.auth.mode:local}")
    private String authMode = "local";

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfile()) {
            return;
        }

        List<String> errors = new ArrayList<>();
        validateTokenSecret(errors);
        validateRsaPrivateKey(errors);
        validateKeylo(errors);
        validateKeyloProvisioning(errors);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Keystone production configuration validation failed:\n - "
                + String.join("\n - ", errors));
        }
    }

    private boolean isProdProfile() {
        for (String activeProfile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(activeProfile)) {
                return true;
            }
        }
        return false;
    }

    private void validateTokenSecret(List<String> errors) {
        if (!StringUtils.hasText(tokenSecret)) {
            missing(errors, "token.secret", "TOKEN_SECRET", "required for Keystone token signing");
            return;
        }
        if (DEFAULT_TOKEN_SECRET.equals(tokenSecret) || DOCKER_PLACEHOLDER_TOKEN_SECRET.equals(tokenSecret)
            || "changeme-please-update-in-production".equals(tokenSecret)) {
            unsafe(errors, "token.secret", "TOKEN_SECRET", "must not use the default development or docker placeholder secret");
        }
        if (tokenSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            unsafe(errors, "token.secret", "TOKEN_SECRET", "must be at least 32 bytes");
        }
    }

    private void validateRsaPrivateKey(List<String> errors) {
        if (!StringUtils.hasText(rsaPrivateKey)) {
            missing(errors, "keystone.rsaPrivateKey", "KEYSTONE_RSA_PRIVATE_KEY", "required for password decryption");
            return;
        }
        if (rsaPrivateKey.startsWith(DEFAULT_RSA_PRIVATE_KEY_PREFIX)) {
            unsafe(errors, "keystone.rsaPrivateKey", "KEYSTONE_RSA_PRIVATE_KEY", "must not use the bundled sample key");
        }
    }

    private void validateKeylo(List<String> errors) {
        if (!isKeyloAuthMode()) {
            return;
        }
        if (!keyloProperties.isEnabled()) {
            missing(errors, "keystone.auth.keylo.enabled", "KEYSTONE_AUTH_KEYLO_ENABLED",
                "required when Keystone auth mode is " + authMode);
            return;
        }
        if (!StringUtils.hasText(keyloProperties.getBaseUrl())) {
            missing(errors, "keystone.auth.keylo.base-url", "KEYLO_BASE_URL",
                "required when Keylo auth is enabled");
        }
        if (trustedIssuers().isEmpty()) {
            missing(errors, "keystone.auth.keylo.trusted-issuers", "KEYLO_TRUSTED_ISSUERS",
                "must contain at least one trusted issuer matching the token iss claim");
        }
        if (!StringUtils.hasText(keyloProperties.getJwkSetUri())) {
            missing(errors, "keystone.auth.keylo.jwk-set-uri", "KEYLO_JWK_SET_URI",
                "required when Keylo auth is enabled");
        }
        if (trustedAudiences().isEmpty()) {
            missing(errors, "keystone.auth.keylo.audiences", "KEYLO_AUDIENCES",
                "must contain at least one trusted audience when Keylo auth is enabled");
        }
        if (!StringUtils.hasText(keyloProperties.getCredentialVerifyUrl())) {
            missing(errors, "keystone.auth.keylo.credential-verify-url", "KEYLO_CREDENTIAL_VERIFY_URL",
                "required when Keylo credential login is enabled");
        }
    }

    private boolean isKeyloAuthMode() {
        return !"local".equalsIgnoreCase(authMode);
    }

    private List<String> trustedAudiences() {
        return normalize(keyloProperties.getAudiences());
    }

    private List<String> trustedIssuers() {
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

    private void validateKeyloProvisioning(List<String> errors) {
        if (!keyloProvisioningProperties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(keyloProvisioningProperties.getCreateUserUrl())) {
            missing(errors, "keystone.auth.keylo.provisioning.create-user-url", "KEYLO_CREATE_USER_URL",
                "required when Keylo provisioning is enabled");
        }
        if (!StringUtils.hasText(keyloProvisioningProperties.getAdminTokenUrl())) {
            missing(errors, "keystone.auth.keylo.provisioning.admin-token-url", "KEYLO_ADMIN_TOKEN_URL",
                "required when Keylo provisioning is enabled");
        }
        if (!StringUtils.hasText(keyloProvisioningProperties.getAdminClientId())) {
            missing(errors, "keystone.auth.keylo.provisioning.admin-client-id", "KEYLO_ADMIN_CLIENT_ID",
                "required when Keylo provisioning is enabled");
        }
        String adminClientSecret = keyloProvisioningProperties.getAdminClientSecret();
        if (!StringUtils.hasText(adminClientSecret)) {
            missing(errors, "keystone.auth.keylo.provisioning.admin-client-secret", "KEYLO_ADMIN_CLIENT_SECRET",
                "required when Keylo provisioning is enabled");
        } else if (DEFAULT_KEYLO_ADMIN_CLIENT_SECRET.equals(adminClientSecret)) {
            unsafe(errors, "keystone.auth.keylo.provisioning.admin-client-secret", "KEYLO_ADMIN_CLIENT_SECRET",
                "must not use the default placeholder");
        }
    }

    private void missing(List<String> errors, String propertyName, String environmentVariable, String reason) {
        errors.add("missing required config property '" + propertyName + "'"
            + " (env: " + environmentVariable + ") - " + reason);
    }

    private void unsafe(List<String> errors, String propertyName, String environmentVariable, String reason) {
        errors.add("unsafe config property '" + propertyName + "'"
            + " (env: " + environmentVariable + ") - " + reason);
    }
}
