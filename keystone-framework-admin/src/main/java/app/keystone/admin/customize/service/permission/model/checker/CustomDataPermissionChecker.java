package app.keystone.admin.customize.service.permission.model.checker;

import app.keystone.infrastructure.user.web.SystemLoginUser;
import app.keystone.admin.customize.service.permission.model.AbstractDataPermissionChecker;
import app.keystone.admin.customize.service.permission.model.DataCondition;
import app.keystone.domain.system.dept.db.SysDeptService;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 数据权限测试接口
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomDataPermissionChecker extends AbstractDataPermissionChecker {

    private SysDeptService deptService;


    @Override
    public boolean check(SystemLoginUser loginUser, DataCondition condition) {
        if (condition == null || loginUser == null) {
            return false;
        }

        if (loginUser.getRoleInfo() == null) {
            return false;
        }

        Set<Long> deptIdSet = loginUser.getRoleInfo().getDeptIdSet();
        Long targetDeptId = condition.getTargetDeptId();

        return targetDeptId != null && deptIdSet != null && deptIdSet.contains(targetDeptId);
    }
}
