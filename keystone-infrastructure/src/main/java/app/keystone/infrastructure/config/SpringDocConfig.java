package app.keystone.infrastructure.config;

import app.keystone.common.config.KeystoneConfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author likanug
 * SpringDoc API文档相关配置
 */
@Configuration
public class SpringDocConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/health",
        "/getConfig",
        "/captchaImage",
        "/login/rsa-public-key",
        "/login",
        "/register"
    );

    @Bean
    public OpenAPI keystoneApi(KeystoneConfig keystoneConfig) {
        return new OpenAPI()
            .servers(List.of(new Server().url("/api").description("Frontend API proxy")))
            .info(new Info().title("Keystone 后台管理系统")
                .description("Keystone API")
                .version(keystoneConfig.getVersion()))
            .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer securityOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperations().forEach(operation -> {
                if (PUBLIC_PATHS.contains(path)) {
                    operation.setSecurity(List.of());
                    return;
                }
                operation.setSecurity(List.of(new SecurityRequirement().addList(SECURITY_SCHEME_NAME)));
            }));
        };
    }

}
