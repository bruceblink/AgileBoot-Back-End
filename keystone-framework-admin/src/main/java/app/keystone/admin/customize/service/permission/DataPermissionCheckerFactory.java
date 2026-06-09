package app.keystone.admin.customize.service.permission;

import app.keystone.admin.customize.service.permission.model.AbstractDataPermissionChecker;
import app.keystone.admin.customize.service.permission.model.checker.AllDataPermissionChecker;
import app.keystone.admin.customize.service.permission.model.checker.CustomDataPermissionChecker;
import app.keystone.admin.customize.service.permission.model.checker.DefaultDataPermissionChecker;
import app.keystone.admin.customize.service.permission.model.checker.DeptTreeDataPermissionChecker;
import app.keystone.admin.customize.service.permission.model.checker.OnlySelfDataPermissionChecker;
import app.keystone.admin.customize.service.permission.model.checker.SingleDeptDataPermissionChecker;
import app.keystone.domain.system.dept.db.SysDeptService;
import app.keystone.infrastructure.user.web.DataScopeEnum;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import org.springframework.stereotype.Component;

/**
 * 数据权限检测器工厂
 * @author likanug
 */
@Component
public class DataPermissionCheckerFactory {
    private final AbstractDataPermissionChecker allChecker;
    private final AbstractDataPermissionChecker customChecker;
    private final AbstractDataPermissionChecker singleDeptChecker;
    private final AbstractDataPermissionChecker deptTreeChecker;
    private final AbstractDataPermissionChecker onlySelfChecker;
    private final AbstractDataPermissionChecker defaultSelfChecker;

    public DataPermissionCheckerFactory(SysDeptService deptService) {
        this.allChecker = new AllDataPermissionChecker();
        this.customChecker = new CustomDataPermissionChecker(deptService);
        this.singleDeptChecker = new SingleDeptDataPermissionChecker(deptService);
        this.deptTreeChecker = new DeptTreeDataPermissionChecker(deptService);
        this.onlySelfChecker = new OnlySelfDataPermissionChecker(deptService);
        this.defaultSelfChecker = new DefaultDataPermissionChecker();
    }

    public AbstractDataPermissionChecker getChecker(SystemLoginUser loginUser) {
        if (loginUser == null) {
            return deptTreeChecker;
        }

        DataScopeEnum dataScope = loginUser.getRoleInfo().getDataScope();
        switch (dataScope) {
            case ALL:
                return allChecker;
            case CUSTOM_DEFINE:
                return customChecker;
            case SINGLE_DEPT:
                return singleDeptChecker;
            case DEPT_TREE:
                return deptTreeChecker;
            case ONLY_SELF:
                return onlySelfChecker;
            default:
                return defaultSelfChecker;
        }
    }

}
