package app.keystone.admin.customize.service.permission;

import app.keystone.infrastructure.user.AuthenticationUtils;
import app.keystone.infrastructure.user.web.RoleInfo;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 *
 * @author likanug
 */
@Service("permission")
public class MenuPermissionService {


    /**
     * 验证用户是否具备某权限
     *
     * @param permission 权限字符串
     * @return 用户是否具备某权限
     */
    public boolean has(String permission) {
        if (permission == null || permission.isEmpty()) {
            return false;
        }
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        if (loginUser == null || loginUser.getRoleInfo() == null || loginUser.getRoleInfo().getMenuPermissions() == null
            || loginUser.getRoleInfo().getMenuPermissions().isEmpty()) {
            return false;
        }
        return has(loginUser.getRoleInfo().getMenuPermissions(), permission);
    }


    /**
     * 判断是否包含权限
     *
     * @param permissions 权限列表
     * @param permission 权限字符串
     * @return 用户是否具备某权限
     */
    private boolean has(Set<String> permissions, String permission) {
        return permissions.contains(RoleInfo.ALL_PERMISSIONS) || permissions.contains(permission.trim());
    }

}
