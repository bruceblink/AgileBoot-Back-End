package app.keystone.domain.system.config.model;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.domain.system.config.db.SysConfigEntity;
import app.keystone.domain.system.config.db.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 配置模型工厂
 * @author likanug
 */
@Component
@RequiredArgsConstructor
public class ConfigModelFactory {

    private final SysConfigService configService;

    public ConfigModel loadById(Long configId) {
        SysConfigEntity byId = configService.getById(configId);
        if (byId == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, configId, "参数配置");
        }
        return new ConfigModel(byId, configService);
    }

    public ConfigModel create() {
        return new ConfigModel(configService);
    }


}
