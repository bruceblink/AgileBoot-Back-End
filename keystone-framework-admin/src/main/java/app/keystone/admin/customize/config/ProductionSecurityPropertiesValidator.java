package app.keystone.admin.customize.config;

import java.util.ArrayList;
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

    private static final String DEFAULT_RSA_PRIVATE_KEY_PREFIX = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8";

    private final Environment environment;

    @Value("${keystone.rsaPrivateKey:}")
    private String rsaPrivateKey;

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfile()) {
            return;
        }

        List<String> errors = new ArrayList<>();
        validateRsaPrivateKey(errors);

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

    private void validateRsaPrivateKey(List<String> errors) {
        if (!StringUtils.hasText(rsaPrivateKey)) {
            missing(errors, "keystone.rsaPrivateKey", "KEYSTONE_RSA_PRIVATE_KEY",
                "required for password decryption and Keystone JWT signing");
            return;
        }
        if (rsaPrivateKey.startsWith(DEFAULT_RSA_PRIVATE_KEY_PREFIX)) {
            unsafe(errors, "keystone.rsaPrivateKey", "KEYSTONE_RSA_PRIVATE_KEY", "must not use the bundled sample key");
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
