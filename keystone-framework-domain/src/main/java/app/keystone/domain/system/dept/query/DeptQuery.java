package app.keystone.domain.system.dept.query;

import app.keystone.common.core.page.AbstractQuery;
import app.keystone.domain.system.dept.db.SysDeptEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
public class DeptQuery extends AbstractQuery<SysDeptEntity> {

    private Long deptId;

    private Long parentId;

    private Integer status;

    private String deptName;


    @Override
    public QueryWrapper<SysDeptEntity> addQueryCondition() {
        return new QueryWrapper<SysDeptEntity>()
            .eq(status != null, "status", status)
            .eq(parentId != null, "parent_id", parentId)
            .like(StringUtils.isNotEmpty(deptName), "dept_name", deptName);
    }
}
