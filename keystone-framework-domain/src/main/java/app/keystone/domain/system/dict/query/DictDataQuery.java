package app.keystone.domain.system.dict.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.dict.db.SysDictDataEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * 字典数据查询参数
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Schema(name = "字典数据查询参数")
public class DictDataQuery extends AbstractPageQuery<SysDictDataEntity> {

    @Schema(description = "字典类型")
    private String dictType;

    @Schema(description = "字典标签")
    private String dictLabel;

    @Schema(description = "状态")
    private Integer status;

    @Override
    public QueryWrapper<SysDictDataEntity> addQueryCondition() {
        this.timeRangeColumn = "create_time";
        return new QueryWrapper<SysDictDataEntity>()
            .eq(StringUtils.isNotEmpty(dictType), "dict_type", dictType)
            .like(StringUtils.isNotEmpty(dictLabel), "dict_label", likeValue(dictLabel))
            .eq(status != null, "status", status)
            .orderByAsc("dict_sort");
    }
}
