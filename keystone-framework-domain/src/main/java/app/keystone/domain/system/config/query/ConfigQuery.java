package app.keystone.domain.system.config.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.config.db.SysConfigEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Schema(name = "配置查询参数")
public class ConfigQuery extends AbstractPageQuery<SysConfigEntity> {

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "配置key")
    private String configKey;

    @Schema(description = "是否允许更改配置")
    private Boolean isAllowChange;


    @Override
    public QueryWrapper<SysConfigEntity> addQueryCondition() {
        QueryWrapper<SysConfigEntity> queryWrapper = new QueryWrapper<SysConfigEntity>()
            .like(StringUtils.isNotEmpty(configName), "config_name", likeValue(configName))
            .eq(StringUtils.isNotEmpty(configKey), "config_key", configKey)
            .eq(isAllowChange != null, "is_allow_change", isAllowChange);

        this.timeRangeColumn = "create_time";

        return queryWrapper;
    }
}
