package app.keystone.infrastructure.config.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class EncryptedRedisPasswordEnvironmentPostProcessorTest {

    @Test
    void decryptsSecretV1RedisPassword() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
            "keystone.redis.password-encryption.enabled", "true",
            "keystone.redis.password-encryption.encrypt-key", "oN06GRBVOr2G8lFxKisSmnONozK0Ru8z9Og2q7Bsbww=",
            "spring.data.redis.password",
            "secret:v1:aes-256-gcm:Mq+2ogeNoFKYIQYe:ZXSfffeenjQZTYXIRWJpH2xaBZgiRBAiuv4qVpzIZMrZy/B/rw=="
        )));

        new EncryptedRedisPasswordEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertEquals("python-rust-db-secret", environment.getProperty("spring.data.redis.password"));
    }

    @Test
    void decryptsSecretV1RedisPasswordFromFiles() throws Exception {
        Path tempDir = Files.createTempDirectory("keystone-redis-secret");
        Path passwordFile = tempDir.resolve(".redis_password.enc");
        Path keyFile = tempDir.resolve(".redis_password.key");
        Files.writeString(passwordFile,
            "secret:v1:aes-256-gcm:Mq+2ogeNoFKYIQYe:ZXSfffeenjQZTYXIRWJpH2xaBZgiRBAiuv4qVpzIZMrZy/B/rw==");
        Files.writeString(keyFile, "oN06GRBVOr2G8lFxKisSmnONozK0Ru8z9Og2q7Bsbww=");
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
            "keystone.redis.password-encryption.enabled", "true",
            "keystone.redis.password-encryption.encrypt-key-file", keyFile.toString(),
            "spring.data.redis.password-file", passwordFile.toString()
        )));

        new EncryptedRedisPasswordEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertEquals("python-rust-db-secret", environment.getProperty("spring.data.redis.password"));
    }
}
