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
    void shouldAllowAnonymousSwaggerAccessOutsideProd() throws Exception {
        SecurityConfig securityConfig = securityConfigWithProfile("dev");

        assertThat(securityConfig.isSwaggerAuthenticationRequired()).isFalse();
    }

    @Test
    void shouldRequireAuthenticationForSwaggerAccessInProd() throws Exception {
        SecurityConfig securityConfig = securityConfigWithProfile("prod");

        assertThat(securityConfig.isSwaggerAuthenticationRequired()).isTrue();
    }

    private SecurityConfig securityConfigWithProfile(String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
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
