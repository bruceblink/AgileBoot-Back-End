package app.keystone.domain.system.post.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.post.db.SysPostEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/**
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PostQuery extends AbstractPageQuery<SysPostEntity> {

    private String postCode;
    private String postName;
    @Min(value = 0, message = "岗位状态值无效")
    @Max(value = 1, message = "岗位状态值无效")
    private Integer status;

    @Override
    public QueryWrapper<SysPostEntity> addQueryCondition() {
        QueryWrapper<SysPostEntity> queryWrapper = new QueryWrapper<SysPostEntity>()
            .eq(status != null, "status", status)
            .eq(StringUtils.isNotEmpty(postCode), "post_code", postCode)
            .like(StringUtils.isNotEmpty(postName), "post_name", likeValue(postName));
        // 当前端没有选择排序字段时，则使用post_sort字段升序排序（在父类AbstractQuery中默认为升序）
        if (StringUtils.isEmpty(this.getOrderColumn())) {
            this.setOrderColumn("post_sort");
        }
        this.setTimeRangeColumn("create_time");

        return queryWrapper;
    }
}
