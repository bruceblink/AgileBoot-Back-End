package app.keystone.framework.starter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KeystoneFrameworkStarterDependencyTest {

    private static final String AUTO_CONFIGURATION_IMPORTS =
        "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

    @Test
    @DisplayName("starter 依赖应暴露框架自动配置")
    void starterDependency_shouldExposeFrameworkAutoConfigurations()
        throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(AUTO_CONFIGURATION_IMPORTS);
        List<String> imports = new ArrayList<>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
                reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(imports::add);
            }
        }

        assertThat(imports)
            .contains(
                "app.keystone.framework.admin.autoconfigure.KeystoneFrameworkAdminAutoConfiguration",
                "app.keystone.framework.domain.autoconfigure.KeystoneFrameworkDomainAutoConfiguration",
                "app.keystone.framework.domain.autoconfigure.KeystoneFrameworkInfrastructureAutoConfiguration"
            );
    }

    @Test
    @DisplayName("starter 依赖不应携带应用迁移资源")
    void starterDependency_shouldNotCarryApplicationMigrations() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        assertThat(classLoader.getResource("db/migrate/mysql/V3_3_1__init_core_schema_data.sql"))
            .isNotNull();
        assertThat(classLoader.getResource("db/migrate/app/mysql/V3_5_0__add_application_table.sql"))
            .isNull();
        assertThat(classLoader.getResource("db/migrate/app/mysql/V999__example_application_table.sql"))
            .isNull();
        assertThat(classLoader.getResource("db/migrate/h2/keystone_schema.sql"))
            .isNull();
    }
}
