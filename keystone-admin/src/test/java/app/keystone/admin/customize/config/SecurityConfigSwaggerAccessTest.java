package app.keystone.admin.customize.config;

import static org.assertj.core.api.Assertions.assertThat;

import app.keystone.admin.customize.service.login.TokenService;
import app.keystone.domain.common.cache.RedisCacheService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.CorsFilter;

class SecurityConfigSwaggerAccessTest {

    @Test
    void shouldAllowSwaggerAccessWhenSpringdocIsEnabled() {
        SecurityConfig securityConfig = securityConfigWithSpringdocEnabled(true, true);

        assertThat(securityConfig.isSwaggerEnabled()).isTrue();
    }

    @Test
    void shouldDenySwaggerAccessWhenApiDocsDisabled() {
        SecurityConfig securityConfig = securityConfigWithSpringdocEnabled(false, true);

        assertThat(securityConfig.isSwaggerEnabled()).isFalse();
    }

    @Test
    void shouldDenySwaggerAccessWhenSwaggerUiDisabled() {
        SecurityConfig securityConfig = securityConfigWithSpringdocEnabled(true, false);

        assertThat(securityConfig.isSwaggerEnabled()).isFalse();
    }

    @Test
    void shouldDenySwaggerAccessWhenSpringdocIsUnset() {
        SecurityConfig securityConfig = securityConfigWithSpringdocEnabled(false, false);

        assertThat(securityConfig.isSwaggerEnabled()).isFalse();
    }

    private SecurityConfig securityConfigWithSpringdocEnabled(boolean apiDocsEnabled, boolean swaggerUiEnabled) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("springdoc.api-docs.enabled", Boolean.toString(apiDocsEnabled));
        environment.setProperty("springdoc.swagger-ui.enabled", Boolean.toString(swaggerUiEnabled));
        return new SecurityConfig(
            Mockito.mock(TokenService.class),
            Mockito.mock(RedisCacheService.class),
            Mockito.mock(JwtAuthenticationTokenFilter.class),
            Mockito.mock(UserDetailsService.class),
            Mockito.mock(CorsFilter.class),
            environment
        );
    }
}
