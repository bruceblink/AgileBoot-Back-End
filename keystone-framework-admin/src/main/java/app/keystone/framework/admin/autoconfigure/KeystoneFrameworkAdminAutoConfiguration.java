package app.keystone.framework.admin.autoconfigure;

import app.keystone.framework.domain.autoconfigure.KeystoneFrameworkDomainAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@AutoConfiguration(after = KeystoneFrameworkDomainAutoConfiguration.class)
@ConditionalOnProperty(prefix = "keystone.framework.admin", name = "enabled", havingValue = "true",
    matchIfMissing = true)
@Import(KeystoneFrameworkDomainAutoConfiguration.class)
@ComponentScan(basePackages = {
    "app.keystone.admin.controller.common",
    "app.keystone.admin.controller.system",
    "app.keystone.admin.customize"
})
public class KeystoneFrameworkAdminAutoConfiguration {
}
