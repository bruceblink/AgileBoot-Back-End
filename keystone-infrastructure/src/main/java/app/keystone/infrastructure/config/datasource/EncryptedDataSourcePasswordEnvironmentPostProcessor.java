package app.keystone.infrastructure.config.datasource;

import app.keystone.infrastructure.security.SecretValueDecryptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * Decrypts datasource password secrets before dynamic datasource binds properties.
 */
public class EncryptedDataSourcePasswordEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "KeystoneDataSourcePasswordDecrypt";
    private static final Pattern DATA_SOURCE_PASSWORD_KEY = Pattern.compile(
        "^spring\\.datasource(\\.dynamic\\.datasource\\.[^.]+)?\\.password$"
    );
    private static final Pattern DATA_SOURCE_PASSWORD_FILE_KEY = Pattern.compile(
        "^spring\\.datasource(\\.dynamic\\.datasource\\.[^.]+)?\\.password-file$"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String encryptKey = resolveEncryptKey(environment);

        MutablePropertySources propertySources = environment.getPropertySources();
        Map<String, Object> decryptedMap = new LinkedHashMap<>();
        boolean foundEncryptedPassword = false;

        for (PropertySource<?> source : propertySources) {
            if (!(source instanceof EnumerablePropertySource<?> enumerablePropertySource)) {
                continue;
            }
            for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                PasswordValue passwordValue = resolvePasswordValue(environment, propertyName);
                if (passwordValue == null) {
                    continue;
                }
                String value = passwordValue.value();
                if (!SecretValueDecryptor.isSecretV1(value)) {
                    throw new IllegalStateException(
                        "Datasource password must use secret:v1:aes-256-gcm format when encryption is enabled: "
                            + passwordValue.targetPropertyName()
                    );
                }

                foundEncryptedPassword = true;
                if (encryptKey == null || encryptKey.trim().isEmpty()) {
                    throw new IllegalStateException(
                        "Database password is encrypted but encrypt key is missing: keystone.datasource.password-encryption.encrypt-key"
                    );
                }

                try {
                    String decrypted = decryptValue(value, encryptKey);
                    decryptedMap.put(passwordValue.targetPropertyName(), decrypted);
                } catch (Exception ex) {
                    throw new IllegalStateException(
                        "Failed to decrypt datasource password for property: " + passwordValue.targetPropertyName(), ex);
                }
            }
        }

        if (foundEncryptedPassword && !decryptedMap.isEmpty()) {
            propertySources.addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, decryptedMap));
        }
    }

    private static String resolveEncryptKey(ConfigurableEnvironment environment) {
        String encryptKey = firstText(
            environment.getProperty("keystone.datasource.password-encryption.encrypt-key"),
            environment.getProperty("KEYSTONE_DATASOURCE_ENCRYPT_KEY")
        );
        if (encryptKey != null) {
            return encryptKey;
        }

        String encryptKeyFile = firstText(
            environment.getProperty("keystone.datasource.password-encryption.encrypt-key-file"),
            environment.getProperty("KEYSTONE_DATASOURCE_ENCRYPT_KEY_FILE")
        );
        if (encryptKeyFile == null) {
            return null;
        }

        try {
            return Files.readString(Path.of(encryptKeyFile.trim())).trim();
        } catch (Exception ex) {
            throw new IllegalStateException("Database password encrypt key file cannot be read: " + encryptKeyFile, ex);
        }
    }

    private static PasswordValue resolvePasswordValue(ConfigurableEnvironment environment, String propertyName) {
        if (DATA_SOURCE_PASSWORD_KEY.matcher(propertyName).matches()) {
            return new PasswordValue(propertyName, environment.getProperty(propertyName));
        }

        if (DATA_SOURCE_PASSWORD_FILE_KEY.matcher(propertyName).matches()
            || "SPRING_DATASOURCE_PASSWORD_FILE".equals(propertyName)) {
            String passwordFile = environment.getProperty(propertyName);
            if (passwordFile == null || passwordFile.trim().isEmpty()) {
                return null;
            }
            try {
                return new PasswordValue(targetPasswordProperty(propertyName),
                    Files.readString(Path.of(passwordFile.trim())).trim());
            } catch (Exception ex) {
                throw new IllegalStateException("Database encrypted password file cannot be read: " + passwordFile, ex);
            }
        }

        return null;
    }

    private static String targetPasswordProperty(String filePropertyName) {
        if ("SPRING_DATASOURCE_PASSWORD_FILE".equals(filePropertyName)) {
            return "spring.datasource.dynamic.datasource.master.password";
        }
        return filePropertyName.substring(0, filePropertyName.length() - "-file".length());
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private record PasswordValue(String targetPropertyName, String value) {
    }

    private static String decryptValue(String value, String encryptKey) throws Exception {
        return SecretValueDecryptor.decryptSecretV1(value.trim(), encryptKey);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
