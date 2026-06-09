package app.keystone.domain.system.monitor.dto;

import app.keystone.domain.common.cache.CacheCenter;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import app.keystone.domain.system.dept.db.SysDeptEntity;
import app.keystone.infrastructure.user.base.LoginInfo;
import lombok.Data;

/**
 * 当前在线会话
 *
 * @author ruoyi
 */
@Data
public class OnlineUserDTO {

    /**
     * 会话编号
     */
    private String tokenId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 登录IP地址
     */
    private String ipAddress;

    /**
     * 登录地址
     */
    private String loginLocation;

    /**
     * 浏览器类型
     */
    private String browser;

    /**
     * 操作系统
     */
    private String operationSystem;

    /**
     * 登录时间
     */
    private Long loginTime;


    public OnlineUserDTO(SystemLoginUser user) {
        if (user == null) {
            return;
        }
        this.setTokenId(user.getCachedKey());
        this.tokenId = user.getCachedKey();
        this.username = user.getUsername();
        LoginInfo loginInfo = user.getLoginInfo();
        if (loginInfo != null) {
            this.ipAddress = loginInfo.getIpAddress();
            this.loginLocation = loginInfo.getLocation();
            this.browser = loginInfo.getBrowser();
            this.operationSystem = loginInfo.getOperationSystem();
            this.loginTime = loginInfo.getLoginTime();
        }

        if (user.getDeptId() == null) {
            return;
        }

        SysDeptEntity deptEntity = CacheCenter.deptCache().get(user.getDeptId());

        if (deptEntity != null) {
            this.deptName = deptEntity.getDeptName();
        }
    }

}
