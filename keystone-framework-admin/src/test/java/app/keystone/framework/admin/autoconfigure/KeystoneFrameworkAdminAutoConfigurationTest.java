package app.keystone.framework.admin.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;

class KeystoneFrameworkAdminAutoConfigurationTest {

    @Test
    void autoConfigurationImports_shouldRegisterLoadableConfigurationClass()
        throws IOException, ClassNotFoundException {
        ClassPathResource resource = new ClassPathResource(
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");

        String imports = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(imports).contains(KeystoneFrameworkAdminAutoConfiguration.class.getName());
        assertThat(Class.forName(KeystoneFrameworkAdminAutoConfiguration.class.getName()))
            .isEqualTo(KeystoneFrameworkAdminAutoConfiguration.class);
    }

    @Test
    @DisplayName("自动配置应扫描框架控制器和 admin 定制包")
    void autoConfiguration_shouldScanFrameworkAdminPackages() {
        ComponentScan componentScan =
            KeystoneFrameworkAdminAutoConfiguration.class.getAnnotation(ComponentScan.class);

        assertThat(componentScan.basePackages())
            .containsExactly(
                "app.keystone.admin.controller.common",
                "app.keystone.admin.controller.system",
                "app.keystone.admin.customize"
            );
    }
}
