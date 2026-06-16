package app.keystone.domain.system.dict.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.dict.db.SysDictTypeEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * 字典类型查询参数
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Schema(name = "字典类型查询参数")
public class DictTypeQuery extends AbstractPageQuery<SysDictTypeEntity> {

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "字典类型")
    private String dictType;

    @Schema(description = "状态")
    @Min(value = 0, message = "字典状态值无效")
    @Max(value = 1, message = "字典状态值无效")
    private Integer status;

    @Override
    public QueryWrapper<SysDictTypeEntity> addQueryCondition() {
        this.timeRangeColumn = "create_time";
        return new QueryWrapper<SysDictTypeEntity>()
            .like(StringUtils.isNotEmpty(dictName), "dict_name", likeValue(dictName))
            .like(StringUtils.isNotEmpty(dictType), "dict_type", likeValue(dictType))
            .eq(status != null, "status", status);
    }
}
