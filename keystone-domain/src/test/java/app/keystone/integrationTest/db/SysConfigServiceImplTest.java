package app.keystone.integrationTest.db;

import app.keystone.common.enums.common.ConfigKeyEnum;
import app.keystone.domain.system.config.db.SysConfigService;
import app.keystone.integrationTest.DockerMySqlIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@DockerMySqlIntegrationTest
class SysConfigServiceImplTest {

    @Resource
    SysConfigService configService;

    @Test
    void testGetConfigValueByKey() {
        String configValue = configService.getConfigValueByKey(ConfigKeyEnum.CAPTCHA.getValue());
        Assertions.assertFalse(Boolean.parseBoolean(configValue));
    }


}
