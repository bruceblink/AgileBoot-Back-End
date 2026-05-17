package app.keystone.infrastructure.config.datasource;

import app.keystone.infrastructure.security.SecretValueDecryptor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * 支持数据库密码配置使用 ENC(...) 的启动期自动解密。
 */
public class EncryptedDataSourcePasswordEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "KeystoneDataSourcePasswordDecrypt";
    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";
    private static final Pattern DATA_SOURCE_PASSWORD_KEY = Pattern.compile(
        "^spring\\.datasource(\\.dynamic\\.datasource\\.[^.]+)?\\.password$"
    );
    private static final Pattern DATA_SOURCE_PASSWORD_FILE_KEY = Pattern.compile(
        "^spring\\.datasource(\\.dynamic\\.datasource\\.[^.]+)?\\.password-file$"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = environment.getProperty(
            "keystone.datasource.password-encryption.enabled",
            Boolean.class,
            false
        );
        if (!enabled) {
            return;
        }

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
                if (!isEncryptedValue(value)) {
                    continue;
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

    private static boolean isEncryptedValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String trimmed = value.trim();
        return SecretValueDecryptor.isSecretV1(trimmed)
            || (trimmed.startsWith(ENC_PREFIX) && trimmed.endsWith(ENC_SUFFIX));
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
        String trimmed = value.trim();
        if (SecretValueDecryptor.isSecretV1(trimmed)) {
            return SecretValueDecryptor.decryptSecretV1(trimmed, encryptKey);
        }

        String encryptedBody = trimmed.substring(ENC_PREFIX.length(), trimmed.length() - ENC_SUFFIX.length());
        return decryptAesBase64(buildLegacyAesKey(encryptKey), encryptedBody);
    }

    private static String decryptAesBase64(byte[] key, String encryptedBase64) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static byte[] buildLegacyAesKey(String encryptKey) {
        byte[] keyBytes = encryptKey.getBytes(StandardCharsets.UTF_8);
        byte[] aesKey = new byte[16];
        int copyLength = Math.min(keyBytes.length, aesKey.length);
        System.arraycopy(keyBytes, 0, aesKey, 0, copyLength);
        return aesKey;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
