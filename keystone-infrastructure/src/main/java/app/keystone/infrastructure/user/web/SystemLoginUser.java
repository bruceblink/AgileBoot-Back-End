package app.keystone.infrastructure.user.web;

import app.keystone.infrastructure.user.base.BaseLoginUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 登录用户身份权限
 * @author likanug
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SystemLoginUser extends BaseLoginUser {

    private static final long serialVersionUID = 1L;

    private boolean isAdmin;

    private Long deptId;

    private RoleInfo roleInfo;

    /**
     * 当超过这个时间 则触发刷新缓存时间
     */
    private Long autoRefreshCacheTime;


    public SystemLoginUser(Long userId, Boolean isAdmin, String username, String password, RoleInfo roleInfo,
        Long deptId) {
        this.userId = userId;
        this.isAdmin = isAdmin;
        this.username = username;
        this.password = password;
        this.roleInfo = roleInfo == null ? null : new RoleInfo(roleInfo);
        this.deptId = deptId;
    }

    public RoleInfo getRoleInfo() {
        return roleInfo == null ? null : new RoleInfo(roleInfo);
    }

    public void setRoleInfo(RoleInfo roleInfo) {
        this.roleInfo = roleInfo == null ? null : new RoleInfo(roleInfo);
    }

    public Long getRoleId() {
        RoleInfo copiedRoleInfo = getRoleInfo();
        return copiedRoleInfo == null ? null : copiedRoleInfo.getRoleId();
    }

    public Long getDeptId() {
        return deptId;
    }


}
