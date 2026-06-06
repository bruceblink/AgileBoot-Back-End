package app.keystone.domain.system.post.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.post.db.SysPostEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
    private Integer status;

    @Override
    public QueryWrapper<SysPostEntity> addQueryCondition() {
        QueryWrapper<SysPostEntity> queryWrapper = new QueryWrapper<SysPostEntity>()
            .eq(status != null, "status", status)
            .eq(StringUtils.isNotEmpty(postCode), "post_code", postCode)
            .like(StringUtils.isNotEmpty(postName), "post_name", postName);
        // 当前端没有选择排序字段时，则使用post_sort字段升序排序（在父类AbstractQuery中默认为升序）
        if (StringUtils.isEmpty(this.getOrderColumn())) {
            this.setOrderColumn("post_sort");
        }
        this.setTimeRangeColumn("create_time");

        return queryWrapper;
    }
}
