package app.keystone.domain.system.dept.query;

import app.keystone.common.core.page.AbstractQuery;
import app.keystone.domain.system.dept.db.SysDeptEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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

    @Positive(message = "部门ID必须为正数")
    private Long deptId;

    @PositiveOrZero(message = "父部门ID不能小于0")
    private Long parentId;

    @Min(value = 0, message = "部门状态值无效")
    @Max(value = 1, message = "部门状态值无效")
    private Integer status;

    private String deptName;


    @Override
    public QueryWrapper<SysDeptEntity> addQueryCondition() {
        return new QueryWrapper<SysDeptEntity>()
            .eq(status != null, "status", status)
            .eq(parentId != null, "parent_id", parentId)
            .like(StringUtils.isNotEmpty(deptName), "dept_name", likeValue(deptName));
    }
}
