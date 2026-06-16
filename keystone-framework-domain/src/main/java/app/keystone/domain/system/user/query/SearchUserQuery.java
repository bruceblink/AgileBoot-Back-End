package app.keystone.domain.system.user.query;

import app.keystone.common.core.page.AbstractPageQuery;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/**
 * 当出现复用Query的情况，我们需要把泛型加到类本身，通过传入类型 来进行复用
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SearchUserQuery<T> extends AbstractPageQuery<T> {

    @Positive(message = "用户ID必须为正数")
    protected Long userId;
    protected String username;
    @Min(value = 0, message = "用户状态值无效")
    @Max(value = 1, message = "用户状态值无效")
    protected Integer status;
    protected String phoneNumber;
    @Positive(message = "部门ID必须为正数")
    protected Long deptId;

    @Override
    public QueryWrapper<T> addQueryCondition() {
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();

        queryWrapper.like(StringUtils.isNotEmpty(username), "username", likeValue(username))
            .like(StringUtils.isNotEmpty(phoneNumber), "u.phone_number", likeValue(phoneNumber))
            .eq(userId != null, "u.user_id", userId)
            .eq(status != null, "u.status", status)
            .and(deptId != null, o ->
                o.eq("u.dept_id", deptId)
                    .or()
                    .apply("u.dept_id IN ( SELECT t.dept_id FROM sys_dept t WHERE find_in_set(" + deptId
                        + ", ancestors))"));

        // 设置排序字段
        this.timeRangeColumn = "u.create_time";

        return queryWrapper;
    }
}
