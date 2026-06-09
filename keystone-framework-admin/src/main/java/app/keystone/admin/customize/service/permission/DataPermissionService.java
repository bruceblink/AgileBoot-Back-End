package app.keystone.admin.customize.service.permission;

import app.keystone.admin.customize.service.permission.model.AbstractDataPermissionChecker;
import app.keystone.admin.customize.service.permission.model.DataCondition;
import app.keystone.domain.system.user.db.SysUserEntity;
import app.keystone.domain.system.user.db.SysUserService;
import app.keystone.infrastructure.user.AuthenticationUtils;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 数据权限校验服务
 * @author likanug
 */
@Service("dataScope")
@RequiredArgsConstructor
public class DataPermissionService {

    private final SysUserService userService;

    private final DataPermissionCheckerFactory dataPermissionCheckerFactory;

    /**
     * 通过userId 校验当前用户 对 目标用户是否有操作权限
     *
     * @param userId 用户id
     * @return 检验结果
     */
    public boolean checkUserId(Long userId) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        SysUserEntity targetUser = userService.getById(userId);
        if (targetUser == null) {
            return true;
        }
        return checkDataScope(loginUser, targetUser.getDeptId(), userId);
    }

    /**
     * 通过userId 校验当前用户 对 目标用户是否有操作权限
     * @param userIds 用户id列表
     * @return 校验结果
     */
    public boolean checkUserIds(List<Long> userIds) {
        if (userIds != null && !userIds.isEmpty()) {
            for (Long userId : userIds) {
                boolean checkResult = checkUserId(userId);
                if (!checkResult) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean checkDeptId(Long deptId) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        return checkDataScope(loginUser, deptId, null);
    }


    public boolean checkDataScope(SystemLoginUser loginUser, Long targetDeptId, Long targetUserId) {
        DataCondition dataCondition = DataCondition.builder().targetDeptId(targetDeptId).targetUserId(targetUserId).build();
        AbstractDataPermissionChecker checker = dataPermissionCheckerFactory.getChecker(loginUser);
        return checker.check(loginUser, dataCondition);
    }



}
