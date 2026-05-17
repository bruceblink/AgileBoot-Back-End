package app.keystone.infrastructure.config.redis;

import app.keystone.infrastructure.security.SecretValueDecryptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

/**
 * Decrypts Redis password secrets before Spring Data Redis binds properties.
 */
public class EncryptedRedisPasswordEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "KeystoneRedisPasswordDecrypt";
    private static final String REDIS_PASSWORD_PROPERTY = "spring.data.redis.password";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = environment.getProperty(
            "keystone.redis.password-encryption.enabled",
            Boolean.class,
            false
        );
        if (!enabled) {
            return;
        }

        String encryptedPassword = resolveEncryptedPassword(environment);
        if (!StringUtils.hasText(encryptedPassword)) {
            return;
        }
        if (!SecretValueDecryptor.isSecretV1(encryptedPassword)) {
            throw new IllegalStateException("Redis password must use secret:v1:aes-256-gcm format when encryption is enabled");
        }

        String encryptKey = resolveEncryptKey(environment);
        if (!StringUtils.hasText(encryptKey)) {
            throw new IllegalStateException(
                "Redis password is encrypted but encrypt key is missing: keystone.redis.password-encryption.encrypt-key"
            );
        }

        try {
            Map<String, Object> decryptedMap = new LinkedHashMap<>();
            decryptedMap.put(REDIS_PASSWORD_PROPERTY, SecretValueDecryptor.decryptSecretV1(encryptedPassword, encryptKey));
            MutablePropertySources propertySources = environment.getPropertySources();
            propertySources.addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, decryptedMap));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt Redis password", ex);
        }
    }

    private String resolveEncryptedPassword(ConfigurableEnvironment environment) {
        String passwordFile = firstText(
            environment.getProperty("spring.data.redis.password-file"),
            environment.getProperty("SPRING_DATA_REDIS_PASSWORD_FILE"),
            environment.getProperty("KEYSTONE_REDIS_PASSWORD_ENC_FILE")
        );
        if (StringUtils.hasText(passwordFile)) {
            return readFile(passwordFile, "Redis encrypted password file");
        }
        return firstText(
            environment.getProperty(REDIS_PASSWORD_PROPERTY),
            environment.getProperty("SPRING_DATA_REDIS_PASSWORD"),
            environment.getProperty("KEYSTONE_REDIS_PASSWORD_ENC")
        );
    }

    private String resolveEncryptKey(ConfigurableEnvironment environment) {
        String keyFile = firstText(
            environment.getProperty("keystone.redis.password-encryption.encrypt-key-file"),
            environment.getProperty("KEYSTONE_REDIS_ENCRYPT_KEY_FILE")
        );
        if (StringUtils.hasText(keyFile)) {
            return readFile(keyFile, "Redis password encrypt key file");
        }
        return firstText(
            environment.getProperty("keystone.redis.password-encryption.encrypt-key"),
            environment.getProperty("KEYSTONE_REDIS_ENCRYPT_KEY")
        );
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String readFile(String path, String label) {
        try {
            return Files.readString(Path.of(path.trim())).trim();
        } catch (Exception ex) {
            throw new IllegalStateException(label + " cannot be read: " + path, ex);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 21;
    }
}
