package app.keystone.domain.system.config.dto;

import app.keystone.common.enums.BasicEnumUtil;
import app.keystone.common.enums.common.YesOrNoEnum;
import app.keystone.common.utils.jackson.JacksonUtil;
import app.keystone.domain.system.config.db.SysConfigEntity;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * @author likanug
 */
@Data
@Schema(name = "ConfigDTO", description = "配置信息")
public class ConfigDTO {

    public ConfigDTO(SysConfigEntity entity) {
        if (entity != null) {
            configId = entity.getConfigId() + "";
            configName = entity.getConfigName();
            configKey = entity.getConfigKey();
            configValue = entity.getConfigValue();
            configOptions = parseConfigOptions(entity.getConfigOptions());
            isAllowChange = entity.getIsAllowChange() == null ? null : (entity.getIsAllowChange() ? 1 : 0);
            isAllowChangeStr = BasicEnumUtil.getDescriptionByBool(YesOrNoEnum.class, entity.getIsAllowChange());
            remark = entity.getRemark();
            createTime = entity.getCreateTime();
        }
    }

    private String configId;
    private String configName;
    private String configKey;
    private String configValue;
    private List<String> configOptions;
    private Integer isAllowChange;
    private String isAllowChangeStr;
    private String remark;
    private Date createTime;

    private List<String> parseConfigOptions(String rawOptions) {
        if (rawOptions == null || rawOptions.isBlank()) {
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
