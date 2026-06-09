package app.keystone.framework.domain.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ConditionalOnProperty(prefix = "keystone.framework.infrastructure", name = "enabled", havingValue = "true",
    matchIfMissing = true)
@ComponentScan(basePackages = {
    "app.keystone.common.config",
    "app.keystone.infrastructure"
})
public class KeystoneFrameworkInfrastructureAutoConfiguration {
}
