package app.keystone.domain.system.config;

import app.keystone.common.core.page.PageDTO;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.jackson.JacksonUtil;
import app.keystone.domain.common.cache.CacheCenter;
import app.keystone.domain.system.config.command.ConfigAddCommand;
import app.keystone.domain.system.config.command.ConfigUpdateCommand;
import app.keystone.domain.system.config.db.SysConfigEntity;
import app.keystone.domain.system.config.db.SysConfigService;
import app.keystone.domain.system.config.dto.ConfigDTO;
import app.keystone.domain.system.config.model.ConfigModel;
import app.keystone.domain.system.config.model.ConfigModelFactory;
import app.keystone.domain.system.config.query.ConfigQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * @author likanug
 */
@Service
@RequiredArgsConstructor
public class ConfigApplicationService {

    private final ConfigModelFactory configModelFactory;

    private final SysConfigService configService;

    public PageDTO<ConfigDTO> getConfigList(ConfigQuery query) {
        Page<SysConfigEntity> page = configService.page(query.toPage(), query.toQueryWrapper());
        List<ConfigDTO> records = page.getRecords().stream().map(ConfigDTO::new).collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public List<ConfigDTO> exportConfigs(ConfigQuery query) {
        return configService.list(query.toQueryWrapper()).stream().map(ConfigDTO::new).collect(Collectors.toList());
    }

    public ConfigDTO getConfigInfo(Long id) {
        SysConfigEntity byId = configService.getById(id);
        return new ConfigDTO(byId);
    }

    public void addConfig(ConfigAddCommand command) {
        checkConfigKeyUnique(command.getConfigKey());
        List<String> configOptions = command.getConfigOptions() == null
            ? Collections.emptyList()
            : command.getConfigOptions();
        checkConfigValue(command.getConfigValue(), configOptions);

        SysConfigEntity entity = new SysConfigEntity();
        entity.setConfigName(command.getConfigName());
        entity.setConfigKey(command.getConfigKey());
        entity.setConfigOptions(configOptions.isEmpty() ? "" : JacksonUtil.to(configOptions));
        entity.setConfigValue(command.getConfigValue());
        entity.setIsAllowChange(command.getIsAllowChange() != null && command.getIsAllowChange() == 1);
        entity.setRemark(command.getRemark());
        try {
            configService.save(entity);
        } catch (DataIntegrityViolationException | PersistenceException e) {
            throwConfigKeyExistsWhenDuplicateKey(e, command.getConfigKey());
        }
        CacheCenter.configCache().invalidate(command.getConfigKey());
    }

    public void updateConfig(ConfigUpdateCommand updateCommand) {
        ConfigModel configModel = configModelFactory.loadById(updateCommand.getConfigId());
        configModel.loadUpdateCommand(updateCommand);

        configModel.checkCanBeModify();

        configModel.updateById();

        CacheCenter.configCache().invalidate(configModel.getConfigKey());
    }

    public void refreshCaches() {
        CacheCenter.configCache().invalidateAll();
    }

    private void checkConfigKeyUnique(String configKey) {
        long count = configService.count(new LambdaQueryWrapper<SysConfigEntity>()
            .eq(SysConfigEntity::getConfigKey, configKey));
        if (count > 0) {
            throw new ApiException(ErrorCode.Business.CONFIG_KEY_IS_NOT_UNIQUE, configKey);
        }
    }

    private void checkConfigValue(String configValue, List<String> configOptions) {
        if (configValue == null || configValue.isBlank()) {
            throw new ApiException(ErrorCode.Business.CONFIG_VALUE_IS_NOT_ALLOW_TO_EMPTY);
        }
        if (!configOptions.isEmpty() && !configOptions.contains(configValue)) {
            throw new ApiException(ErrorCode.Business.CONFIG_VALUE_IS_NOT_IN_OPTIONS);
        }
    }

    private void throwConfigKeyExistsWhenDuplicateKey(RuntimeException e, String configKey) {
        if (isDuplicateKey(e)) {
            throw new ApiException(e, ErrorCode.Business.CONFIG_KEY_IS_NOT_UNIQUE, configKey);
        }
        throw e;
    }

    private boolean isDuplicateKey(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DuplicateKeyException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null
                && (message.contains("Duplicate entry") || message.contains("Unique index or primary key violation"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
