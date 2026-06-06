package app.keystone.domain.system.config;

import app.keystone.common.core.page.PageDTO;
import app.keystone.domain.common.cache.CacheCenter;
import app.keystone.domain.system.config.command.ConfigUpdateCommand;
import app.keystone.domain.system.config.dto.ConfigDTO;
import app.keystone.domain.system.config.model.ConfigModel;
import app.keystone.domain.system.config.model.ConfigModelFactory;
import app.keystone.domain.system.config.query.ConfigQuery;
import app.keystone.domain.system.config.db.SysConfigEntity;
import app.keystone.domain.system.config.db.SysConfigService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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

    public ConfigDTO getConfigInfo(Long id) {
        SysConfigEntity byId = configService.getById(id);
        return new ConfigDTO(byId);
    }

    public void updateConfig(ConfigUpdateCommand updateCommand) {
        ConfigModel configModel = configModelFactory.loadById(updateCommand.getConfigId());
        configModel.loadUpdateCommand(updateCommand);

        configModel.checkCanBeModify();

        configModel.updateById();

        CacheCenter.configCache().invalidate(configModel.getConfigKey());
    }


}
