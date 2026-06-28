package app.keystone.admin.customize.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecurityPropertiesValidatorTest {

    @Test
    void run_shouldFailFast_whenProdMissingRsaPrivateKey() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "rsaPrivateKey", "");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> validator.run(null));

        assertTrue(exception.getMessage().contains("keystone.rsaPrivateKey"));
        assertTrue(exception.getMessage().contains("KEYSTONE_RSA_PRIVATE_KEY"));
    }

    @Test
    void run_shouldPass_whenProdSecurityPropertiesAreConfigured() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "rsaPrivateKey", "production-rsa-private-key");

        assertDoesNotThrow(() -> validator.run(null));
    }

    @Test
    void run_shouldNotRequireDruidPassword_whenProdDruidMonitorIsEnabled() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("spring.datasource.dynamic.druid.stat-view-servlet.enabled", "true");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "rsaPrivateKey", "production-rsa-private-key");

        assertDoesNotThrow(() -> validator.run(null));
    }

    @Test
    void run_shouldSkipValidation_whenProfileIsNotProd() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);

        assertDoesNotThrow(() -> validator.run(null));
    }

    @Test
    void run_shouldRejectSampleRsaKey_whenProdProfileIsActive() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("spring.datasource.dynamic.druid.stat-view-servlet.enabled", "false");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "rsaPrivateKey", "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8-sample");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> validator.run(null));

        assertTrue(exception.getMessage().contains("keystone.rsaPrivateKey"));
        assertTrue(exception.getMessage().contains("KEYSTONE_RSA_PRIVATE_KEY"));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
