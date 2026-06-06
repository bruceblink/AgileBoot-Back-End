package app.keystone.infrastructure.user.app;

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
public class AppLoginUser extends BaseLoginUser {

    private static final long serialVersionUID = 1L;

    private boolean isVip;


    public AppLoginUser(Long userId, Boolean isVip, String cachedKey) {
        this.cachedKey = cachedKey;
        this.userId = userId;
        this.isVip = isVip;
    }


}
