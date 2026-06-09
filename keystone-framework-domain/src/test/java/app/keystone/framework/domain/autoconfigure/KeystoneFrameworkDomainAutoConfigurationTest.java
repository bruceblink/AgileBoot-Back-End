package app.keystone.framework.domain.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

class KeystoneFrameworkDomainAutoConfigurationTest {

    @Test
    void autoConfigurationImports_shouldRegisterLoadableConfigurationClass()
        throws IOException, ClassNotFoundException {
        ClassPathResource resource = new ClassPathResource(
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");

        String imports = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(imports)
            .contains(KeystoneFrameworkInfrastructureAutoConfiguration.class.getName())
            .contains(KeystoneFrameworkDomainAutoConfiguration.class.getName());
        assertThat(Class.forName(KeystoneFrameworkInfrastructureAutoConfiguration.class.getName()))
            .isEqualTo(KeystoneFrameworkInfrastructureAutoConfiguration.class);
        assertThat(Class.forName(KeystoneFrameworkDomainAutoConfiguration.class.getName()))
            .isEqualTo(KeystoneFrameworkDomainAutoConfiguration.class);
    }

    @Test
    @DisplayName("基础设施自动配置应扫描 common 配置和 infrastructure 组件")
    void infrastructureAutoConfiguration_shouldScanFrameworkInfrastructurePackages() {
        ComponentScan componentScan =
            KeystoneFrameworkInfrastructureAutoConfiguration.class.getAnnotation(ComponentScan.class);

        assertThat(componentScan.basePackages())
            .containsExactly("app.keystone.common.config", "app.keystone.infrastructure");
    }

    @Test
    @DisplayName("自动配置应扫描框架领域组件和 system mapper")
    void autoConfiguration_shouldScanFrameworkDomainPackages() {
        ComponentScan componentScan =
            KeystoneFrameworkDomainAutoConfiguration.class.getAnnotation(ComponentScan.class);
        MapperScan mapperScan = KeystoneFrameworkDomainAutoConfiguration.class.getAnnotation(MapperScan.class);
        Import importAnnotation = KeystoneFrameworkDomainAutoConfiguration.class.getAnnotation(Import.class);

        assertThat(componentScan.basePackages())
            .containsExactly("app.keystone.domain.common", "app.keystone.domain.system");
        assertThat(mapperScan.value()).containsExactly("app.keystone.domain.system");
        assertThat(importAnnotation.value()).containsExactly(KeystoneFrameworkInfrastructureAutoConfiguration.class);
    }
}
