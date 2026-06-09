package app.keystone.admin.customize.service.permission.model.checker;

import app.keystone.infrastructure.user.web.SystemLoginUser;
import app.keystone.admin.customize.service.permission.model.AbstractDataPermissionChecker;
import app.keystone.admin.customize.service.permission.model.DataCondition;
import app.keystone.domain.system.dept.db.SysDeptService;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据权限测试接口
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DefaultDataPermissionChecker extends AbstractDataPermissionChecker {

    private SysDeptService deptService;

    @Override
    public boolean check(SystemLoginUser loginUser, DataCondition condition) {
        return false;
    }

}
