package app.keystone.domain.system.config.model;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.jackson.JacksonUtil;
import app.keystone.domain.system.config.command.ConfigUpdateCommand;
import app.keystone.domain.system.config.db.SysConfigEntity;
import app.keystone.domain.system.config.db.SysConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

/**
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ConfigModel extends SysConfigEntity {

    private SysConfigService configService;

    private Set<String> configOptionSet;

    public ConfigModel(SysConfigService configService) {
        this.configService = configService;
    }

    public ConfigModel(SysConfigEntity entity, SysConfigService configService) {
        BeanUtils.copyProperties(entity, this);

        List<String> options = parseConfigOptions(entity.getConfigOptions());

        this.configOptionSet = new HashSet<>(options);

        this.configService = configService;
    }

    public void loadUpdateCommand(ConfigUpdateCommand updateCommand) {
        this.setConfigValue(updateCommand.getConfigValue());
    }


    public void checkCanBeModify() {
        if (StringUtils.isBlank(getConfigValue())) {
            throw new ApiException(ErrorCode.Business.CONFIG_VALUE_IS_NOT_ALLOW_TO_EMPTY);
        }

        if (!configOptionSet.isEmpty() && !configOptionSet.contains(getConfigValue())) {
            throw new ApiException(ErrorCode.Business.CONFIG_VALUE_IS_NOT_IN_OPTIONS);
        }
    }

    private List<String> parseConfigOptions(String rawOptions) {
        if (StringUtils.isBlank(rawOptions)) {
            return Collections.emptyList();
        }
        try {
            JsonNode node = JacksonUtil.getObjectMapper().readTree(rawOptions);
            if (!node.isArray()) {
                return Collections.emptyList();
            }
            List<String> options = JacksonUtil.fromList(rawOptions, String.class);
            return options == null ? Collections.emptyList() : options;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

}
