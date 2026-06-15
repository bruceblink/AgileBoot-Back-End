package app.keystone.domain.system.role.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.role.db.SysRoleEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/**
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RoleQuery extends AbstractPageQuery<SysRoleEntity> {

    private String roleName;

    private String roleKey;

    private String status;


    @Override
    public QueryWrapper<SysRoleEntity> addQueryCondition() {
        QueryWrapper<SysRoleEntity> queryWrapper = new QueryWrapper<SysRoleEntity>()
            .eq(status != null, "status", status)
            .eq(StringUtils.isNotEmpty(roleKey), "role_key", roleKey)
            .like(StringUtils.isNotEmpty(roleName), "role_name", likeValue(roleName));

//        this.addTimeCondition(queryWrapper, "create_time");

//        this.setOrderColumn("role_sort");
//        this.addSortCondition(queryWrapper);

        return queryWrapper;
    }
}
