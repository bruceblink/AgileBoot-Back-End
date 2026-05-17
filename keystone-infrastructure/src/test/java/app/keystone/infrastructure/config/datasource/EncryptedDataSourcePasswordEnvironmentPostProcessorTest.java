package app.keystone.infrastructure.config.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class EncryptedDataSourcePasswordEnvironmentPostProcessorTest {

    @Test
    void decryptsSecretV1DataSourcePassword() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
            "keystone.datasource.password-encryption.enabled", "true",
            "keystone.datasource.password-encryption.encrypt-key", "oN06GRBVOr2G8lFxKisSmnONozK0Ru8z9Og2q7Bsbww=",
            "spring.datasource.dynamic.datasource.master.password",
            "secret:v1:aes-256-gcm:Mq+2ogeNoFKYIQYe:ZXSfffeenjQZTYXIRWJpH2xaBZgiRBAiuv4qVpzIZMrZy/B/rw=="
        )));

        new EncryptedDataSourcePasswordEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertEquals("python-rust-db-secret",
            environment.getProperty("spring.datasource.dynamic.datasource.master.password"));
    }

    @Test
    void decryptsSecretV1DataSourcePasswordFromFiles() throws Exception {
        Path tempDir = Files.createTempDirectory("keystone-datasource-secret");
        Path passwordFile = tempDir.resolve(".database_password.enc");
        Path keyFile = tempDir.resolve(".database_password.key");
        Files.writeString(passwordFile,
            "secret:v1:aes-256-gcm:Mq+2ogeNoFKYIQYe:ZXSfffeenjQZTYXIRWJpH2xaBZgiRBAiuv4qVpzIZMrZy/B/rw==");
        Files.writeString(keyFile, "oN06GRBVOr2G8lFxKisSmnONozK0Ru8z9Og2q7Bsbww=");
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
            "keystone.datasource.password-encryption.enabled", "true",
            "keystone.datasource.password-encryption.encrypt-key-file", keyFile.toString(),
            "spring.datasource.dynamic.datasource.master.password-file", passwordFile.toString()
        )));

        new EncryptedDataSourcePasswordEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertEquals("python-rust-db-secret",
            environment.getProperty("spring.datasource.dynamic.datasource.master.password"));
    }
}
