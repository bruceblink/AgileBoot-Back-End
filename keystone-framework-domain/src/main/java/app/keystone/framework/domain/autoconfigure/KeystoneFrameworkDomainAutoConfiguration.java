package app.keystone.framework.domain.autoconfigure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@AutoConfiguration(after = KeystoneFrameworkInfrastructureAutoConfiguration.class)
@ConditionalOnProperty(prefix = "keystone.framework.domain", name = "enabled", havingValue = "true",
    matchIfMissing = true)
@Import(KeystoneFrameworkInfrastructureAutoConfiguration.class)
@ComponentScan(basePackages = {
    "app.keystone.domain.common",
    "app.keystone.domain.system"
})
@MapperScan(value = "app.keystone.domain.system", markerInterface = BaseMapper.class)
public class KeystoneFrameworkDomainAutoConfiguration {
}
